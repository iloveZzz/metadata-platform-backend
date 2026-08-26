package com.yss.datamiddle.aicontextlayer;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.AgentCredential;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ConnectionAttempt;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ConnectionAuthenticator;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.CredentialStatus;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpSession;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpSessionStatus;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.AgentCredentialGateway;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.CredentialCipher;
import com.yss.datamiddle.aicontextlayer.repository.AgentCredentialRepository;
import com.yss.datamiddle.aicontextlayer.repository.entity.AgentCredentialPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 吊销即时生效 + 凭据持久化接线测试（WU-01-03，DB-backed，替换 InMemory seam）。
 *
 * <p>以真实 H2（MySQL 模式）数据源 + agent_credential / mcp_session 表运行：
 * 凭据密文引用落库（credential_ref 不存明文，SEC-05 / E6 落库检查）、
 * 有效凭据建立会话、无效 / 过期 / 已吊销统一 unauthorized（SEC-05）、
 * 已吊销凭据的活跃会话强制断开（吊销即时生效）。</p>
 */
@SpringBootTest(classes = com.yss.metadata.MetadataPlatformApplication.class, properties = {
    "spring.datasource.primary.url=jdbc:h2:mem:ai_context_layer;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.primary.driver-class-name=org.h2.Driver",
    "spring.datasource.primary.username=sa",
    "spring.datasource.primary.password=",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
    "spring.liquibase.enabled=false"
})
class CredentialRevocationTest {

    private static final Instant NOW = Instant.parse("2026-08-11T08:00:00Z");
    private static final String SECRET = "agent-1-valid-secret";

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ConnectionAuthenticator authenticator;
    @Autowired
    private AgentCredentialGateway agentCredentialGateway;
    @Autowired
    private AgentCredentialRepository agentCredentialRepository;
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
    void credentialRefIsStoredAsCiphertextNotPlaintext() {
        // SEC-05 / E6 落库检查：credential_ref 为 KMS 密文引用，不存明文
        seedActiveCredential("agent-1", "v1", SECRET);

        String storedRef = jdbcTemplate.queryForObject(
            "SELECT credential_ref FROM agent_credential WHERE agent_id = ?",
            String.class, "agent-1");

        assertThat(storedRef).isNotEqualTo(SECRET);
        assertThat(storedRef).doesNotContain(SECRET);
        assertThat(storedRef).startsWith("local:v1:");
    }

    @Test
    void validCredentialEstablishesSessionPersistedToDb() {
        seedActiveCredential("agent-1", "v1", SECRET);

        McpSession session = authenticateOk(SECRET);

        assertThat(session.getAgentId()).isEqualTo("agent-1");
        assertThat(session.getStatus()).isEqualTo(McpSessionStatus.ACTIVE);
        Integer active = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM mcp_session WHERE agent_id = ? AND status = 'ACTIVE'",
            Integer.class, "agent-1");
        assertThat(active).isEqualTo(1);
    }

    @Test
    void invalidSecretRejectsConnectionWithoutSession() {
        seedActiveCredential("agent-1", "v1", SECRET);

        assertUnauthorized("wrong-secret");

        Integer sessionCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM mcp_session", Integer.class);
        assertThat(sessionCount).isZero();
    }

    @Test
    void expiredCredentialRejectsConnectionWithoutSession() {
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

        Integer sessionCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM mcp_session", Integer.class);
        assertThat(sessionCount).isZero();
    }

    @Test
    void revokedCredentialRejectsConnectionAndForceTerminatesActiveSessions() {
        // 吊销即时生效（SEC-05）：agent_credential 状态 REVOKED 检查 + 活跃会话强制断开
        seedActiveCredential("agent-1", "v1", SECRET);
        authenticateOk(SECRET);
        Integer activeBefore = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM mcp_session WHERE agent_id = ? AND status = 'ACTIVE'",
            Integer.class, "agent-1");
        assertThat(activeBefore).isEqualTo(1);

        // 模拟凭据吊销（管理动作，IC-04 归属待确认；此处经 Mapper 置 REVOKED）
        agentCredentialRepository.update(null, Wrappers.<AgentCredentialPO>lambdaUpdate()
            .eq(AgentCredentialPO::getAgentId, "agent-1")
            .eq(AgentCredentialPO::getCredentialVersion, "v1")
            .set(AgentCredentialPO::getStatus, CredentialStatus.REVOKED.name())
            .set(AgentCredentialPO::getRevokedAt, toLocalDateTime(Instant.now())));

        assertUnauthorized(SECRET);

        // 活跃会话已被强制断开（状态流转 ACTIVE → TERMINATED，落库）
        Integer activeAfter = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM mcp_session WHERE agent_id = ? AND status = 'ACTIVE'",
            Integer.class, "agent-1");
        assertThat(activeAfter).isZero();
        Integer terminated = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM mcp_session WHERE agent_id = ? AND status = 'TERMINATED'",
            Integer.class, "agent-1");
        assertThat(terminated).isEqualTo(1);
    }

    @Test
    void revokedCredentialRejectsNewConnection() {
        seedActiveCredential("agent-1", "v1", SECRET);
        agentCredentialRepository.update(null, Wrappers.<AgentCredentialPO>lambdaUpdate()
            .eq(AgentCredentialPO::getAgentId, "agent-1")
            .eq(AgentCredentialPO::getCredentialVersion, "v1")
            .set(AgentCredentialPO::getStatus, CredentialStatus.REVOKED.name()));

        assertUnauthorized(SECRET);
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

    private McpSession authenticateOk(String secret) {
        return authenticator.authenticate(ConnectionAttempt.builder()
            .presentedSecret(secret).build(), Instant.now());
    }

    private void assertUnauthorized(String secret) {
        assertThatThrownBy(() -> authenticator.authenticate(ConnectionAttempt.builder()
            .presentedSecret(secret).build(), Instant.now()))
            .isInstanceOf(McpException.class)
            .satisfies(e -> assertThat(((McpException) e).getErrorCode())
                .isEqualTo(McpErrorCode.UNAUTHORIZED));
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Shanghai"));
    }
}
