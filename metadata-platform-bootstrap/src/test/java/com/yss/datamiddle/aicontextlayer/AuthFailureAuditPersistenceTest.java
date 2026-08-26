package com.yss.datamiddle.aicontextlayer;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.AgentCredential;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ConnectionAttempt;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ConnectionAuthenticator;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.CredentialStatus;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.AgentCredentialGateway;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.CredentialCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 鉴权失败留痕审计接线测试（WU-01-03，SEC-06 调用即写 / 数据架构 §6.1 鉴权失败路径）。
 *
 * <p>四种鉴权失败（缺失 / 无效 / 过期 / 已吊销）均同步写入 audit_log：
 * result_code = unauthorized、tool = 连接级标记、会话不建立；审计行不含凭据明文（SEC-05/11）。</p>
 */
@SpringBootTest(classes = com.yss.metadata.MetadataPlatformApplication.class, properties = {
    "spring.datasource.primary.url=jdbc:h2:mem:ai_context_layer;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.primary.driver-class-name=org.h2.Driver",
    "spring.datasource.primary.username=sa",
    "spring.datasource.primary.password=",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
    "spring.liquibase.enabled=false"
})
class AuthFailureAuditPersistenceTest {

    private static final Instant NOW = Instant.parse("2026-08-11T08:00:00Z");

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ConnectionAuthenticator authenticator;
    @Autowired
    private AgentCredentialGateway agentCredentialGateway;
    @Autowired
    private CredentialCipher credentialCipher;

    @BeforeEach
    void setUp() {
        TestDbSupport.ensureSchema(jdbcTemplate);
        jdbcTemplate.update("DELETE FROM audit_log");
        jdbcTemplate.update("DELETE FROM mcp_session");
        jdbcTemplate.update("DELETE FROM agent_credential");
    }

    @Test
    void invalidCredentialWritesAuthFailureAuditSynchronously() {
        seedActiveCredential("agent-1", "v1", "agent-1-secret");

        assertUnauthorized("wrong-secret");

        List<Map<String, Object>> rows = queryAuditRows();
        assertThat(rows).hasSize(1);
        Map<String, Object> row = rows.get(0);
        assertThat(row.get("result_code")).isEqualTo("unauthorized");
        assertThat(row.get("tool")).isEqualTo("connection");
        assertThat(row.get("session_id")).isEqualTo("no-session");
        // 无效凭据无法识别主体 → unknown 占位
        assertThat(row.get("agent_id")).isEqualTo("unknown");
        assertThat(row.get("mcp_request_id")).isNotNull();
    }

    @Test
    void missingCredentialWritesAuthFailureAudit() {
        assertUnauthorized(null);

        List<Map<String, Object>> rows = queryAuditRows();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("result_code")).isEqualTo("unauthorized");
    }

    @Test
    void revokedCredentialAuditCarriesIdentifiedAgent() {
        seedActiveCredential("agent-1", "v1", "agent-1-secret");
        // 置 REVOKED（模拟吊销管理动作）
        jdbcTemplate.update(
            "UPDATE agent_credential SET status = 'REVOKED', revoked_at = CURRENT_TIMESTAMP"
                + " WHERE agent_id = ? AND credential_version = ?",
            "agent-1", "v1");

        assertUnauthorized("agent-1-secret");

        List<Map<String, Object>> rows = queryAuditRows();
        assertThat(rows).hasSize(1);
        // 已识别主体：agent_id 记录真实主体
        assertThat(rows.get(0).get("agent_id")).isEqualTo("agent-1");
        assertThat(rows.get(0).get("result_code")).isEqualTo("unauthorized");
    }

    @Test
    void expiredCredentialWritesAuthFailureAudit() {
        AgentCredential expired = AgentCredential.builder()
            .agentId("agent-1")
            .credentialVersion("v1")
            .credentialRef(credentialCipher.reference("expired-secret"))
            .status(CredentialStatus.ACTIVE)
            .issuedAt(NOW.minus(30, ChronoUnit.DAYS))
            .expiresAt(NOW.minus(1, ChronoUnit.DAYS))
            .build();
        agentCredentialGateway.addAgentCredential(expired);

        assertUnauthorized("expired-secret");

        List<Map<String, Object>> rows = queryAuditRows();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("result_code")).isEqualTo("unauthorized");
        assertThat(rows.get(0).get("agent_id")).isEqualTo("agent-1");
    }

    @Test
    void auditRowDoesNotContainPresentedSecret() {
        seedActiveCredential("agent-1", "v1", "agent-1-secret");

        assertUnauthorized("wrong-secret");

        List<Map<String, Object>> rows = queryAuditRows();
        for (Map<String, Object> row : rows) {
            for (Object value : row.values()) {
                if (value != null) {
                    assertThat(value.toString()).doesNotContain("wrong-secret");
                    assertThat(value.toString()).doesNotContain("agent-1-secret");
                }
            }
        }
    }

    @Test
    void eachFailureWritesItsOwnAuditRow() {
        seedActiveCredential("agent-1", "v1", "agent-1-secret");

        assertUnauthorized("wrong-1");
        assertUnauthorized("wrong-2");
        assertUnauthorized("wrong-3");

        List<Map<String, Object>> rows = queryAuditRows();
        assertThat(rows).hasSize(3);
        long distinctRequestIds = rows.stream()
            .map(r -> r.get("mcp_request_id"))
            .distinct()
            .count();
        assertThat(distinctRequestIds).isEqualTo(3);
    }

    private void seedActiveCredential(String agentId, String version, String secret) {
        AgentCredential credential = AgentCredential.builder()
            .agentId(agentId)
            .credentialVersion(version)
            .credentialRef(credentialCipher.reference(secret))
            .status(CredentialStatus.ACTIVE)
            .issuedAt(NOW.minus(1, ChronoUnit.DAYS))
            .expiresAt(NOW.plus(30, ChronoUnit.DAYS))
            .build();
        agentCredentialGateway.addAgentCredential(credential);
    }

    private void assertUnauthorized(String secret) {
        assertThatThrownBy(() -> authenticator.authenticate(ConnectionAttempt.builder()
            .presentedSecret(secret).build(), Instant.now()))
            .isInstanceOf(McpException.class)
            .satisfies(e -> assertThat(((McpException) e).getErrorCode())
                .isEqualTo(McpErrorCode.UNAUTHORIZED));
    }

    private List<Map<String, Object>> queryAuditRows() {
        return jdbcTemplate.queryForList(
            "SELECT mcp_request_id, session_id, agent_id, tool, result_code"
                + " FROM audit_log ORDER BY timestamp");
    }
}
