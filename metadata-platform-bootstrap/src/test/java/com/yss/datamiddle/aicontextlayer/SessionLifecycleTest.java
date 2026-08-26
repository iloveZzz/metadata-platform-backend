package com.yss.datamiddle.aicontextlayer;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.AgentCredential;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.CredentialStatus;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpSession;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpSessionStatus;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.SessionManager;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.McpSessionGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 会话生命周期持久化测试（WU-01-04，DB-backed，替换 InMemory seam）。
 *
 * <p>SessionManager 以真实 mcp_session 表运行：建立即持久化（单聚合事务写入）、
 * 会话绑定 Agent 身份与凭据版本、过期 / 空闲回收按运行策略（REC-05：TTL 30min 默认）、
 * 吊销强制断开状态流转（ACTIVE → TERMINATED，幂等）。</p>
 */
@SpringBootTest(classes = com.yss.metadata.MetadataPlatformApplication.class, properties = {
    "spring.datasource.primary.url=jdbc:h2:mem:ai_context_layer;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.primary.driver-class-name=org.h2.Driver",
    "spring.datasource.primary.username=sa",
    "spring.datasource.primary.password=",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
    "spring.liquibase.enabled=false"
})
class SessionLifecycleTest {

    private static final Instant NOW = Instant.parse("2026-08-11T08:00:00Z");

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private SessionManager sessionManager;
    @Autowired
    private McpSessionGateway mcpSessionGateway;

    @BeforeEach
    void setUp() {
        TestDbSupport.ensureSchema(jdbcTemplate);
        jdbcTemplate.update("DELETE FROM audit_log");
        jdbcTemplate.update("DELETE FROM mcp_session");
        jdbcTemplate.update("DELETE FROM agent_credential");
    }

    @Test
    void establishPersistsSessionToDbWithBoundIdentity() {
        McpSession session = sessionManager.establish(activeCredential("agent-1", "v1"), NOW);

        assertThat(session.getStatus()).isEqualTo(McpSessionStatus.ACTIVE);
        MapRow row = querySession(session.getSessionId());
        assertThat(row.agentId).isEqualTo("agent-1");
        assertThat(row.credentialVersion).isEqualTo("v1");
        assertThat(row.status).isEqualTo("ACTIVE");
        assertThat(row.expiresAt).isNotNull();
        assertThat(row.lastActiveAt).isNotNull();
    }

    @Test
    void sessionExpiresAfterMaximumDurationAndIsReclaimed() {
        McpSession session = sessionManager.establish(activeCredential("agent-1", "v1"), NOW);

        // 超过会话最大时长（REC-05，MVP 默认 30 分钟）后不再活跃
        Instant beyondTtl = NOW.plus(SessionManager.DEFAULT_SESSION_TTL)
            .plus(1, ChronoUnit.MINUTES);
        assertThat(session.isActiveAt(beyondTtl)).isFalse();

        int reclaimed = sessionManager.reclaimExpiredSessions(beyondTtl);

        assertThat(reclaimed).isEqualTo(1);
        assertThat(mcpSessionGateway.countActiveSessions("agent-1")).isZero();
        MapRow row = querySession(session.getSessionId());
        assertThat(row.status).isEqualTo("EXPIRED");
    }

    @Test
    void reclaimExpiredIsIdempotentAndNonExpiredSessionsKept() {
        McpSession active = sessionManager.establish(activeCredential("agent-1", "v1"), NOW);
        Instant beforeTtl = NOW.plus(SessionManager.DEFAULT_SESSION_TTL)
            .minus(1, ChronoUnit.MINUTES);

        assertThat(sessionManager.reclaimExpiredSessions(beforeTtl)).isZero();
        assertThat(sessionManager.reclaimExpiredSessions(beforeTtl)).isZero();
        assertThat(mcpSessionGateway.countActiveSessions("agent-1")).isEqualTo(1);
        assertThat(querySession(active.getSessionId()).status).isEqualTo("ACTIVE");
    }

    @Test
    void reclaimedSessionsFreeConcurrencyQuota() {
        for (int i = 0; i < SessionManager.MAX_CONCURRENT_SESSIONS_PER_AGENT; i++) {
            sessionManager.establish(activeCredential("agent-1", "v1"), NOW);
        }
        assertThat(mcpSessionGateway.countActiveSessions("agent-1"))
            .isEqualTo(SessionManager.MAX_CONCURRENT_SESSIONS_PER_AGENT);

        // 全部过期并回收后配额释放，可重新建立
        Instant beyondTtl = NOW.plus(SessionManager.DEFAULT_SESSION_TTL)
            .plus(1, ChronoUnit.MINUTES);
        assertThat(sessionManager.reclaimExpiredSessions(beyondTtl))
            .isEqualTo(SessionManager.MAX_CONCURRENT_SESSIONS_PER_AGENT);
        McpSession session = sessionManager.establish(activeCredential("agent-1", "v1"), NOW);
        assertThat(session.getStatus()).isEqualTo(McpSessionStatus.ACTIVE);
        assertThat(mcpSessionGateway.countActiveSessions("agent-1")).isEqualTo(1);
    }

    @Test
    void forceTerminateByCredentialTransitionsOnlyMatchingDbSessions() {
        McpSession v2a = establishSession("agent-1", "v2");
        McpSession v1 = establishSession("agent-1", "v1");
        McpSession other = establishSession("agent-2", "v2");

        int terminated = sessionManager.forceTerminateByCredential("agent-1", "v2");

        assertThat(terminated).isEqualTo(1);
        assertThat(querySession(v2a.getSessionId()).status).isEqualTo("TERMINATED");
        assertThat(querySession(v1.getSessionId()).status).isEqualTo("ACTIVE");
        assertThat(querySession(other.getSessionId()).status).isEqualTo("ACTIVE");
        assertThat(mcpSessionGateway.countActiveSessions("agent-1")).isEqualTo(1);
    }

    @Test
    void forceTerminateByCredentialIsIdempotent() {
        McpSession session = establishSession("agent-1", "v1");

        assertThat(sessionManager.forceTerminateByCredential("agent-1", "v1")).isEqualTo(1);
        // 幂等：第二次执行无匹配活跃会话，返回 0
        assertThat(sessionManager.forceTerminateByCredential("agent-1", "v1")).isZero();
        assertThat(querySession(session.getSessionId()).status).isEqualTo("TERMINATED");
    }

    private McpSession establishSession(String agentId, String version) {
        return sessionManager.establish(activeCredential(agentId, version), NOW);
    }

    private static AgentCredential activeCredential(String agentId, String version) {
        return AgentCredential.builder()
            .agentId(agentId)
            .credentialVersion(version)
            .status(CredentialStatus.ACTIVE)
            .issuedAt(NOW.minus(1, ChronoUnit.DAYS))
            .expiresAt(NOW.plus(30, ChronoUnit.DAYS))
            .build();
    }

    private MapRow querySession(String sessionId) {
        return jdbcTemplate.queryForObject(
            "SELECT agent_id, credential_version, status, expires_at, last_active_at"
                + " FROM mcp_session WHERE id = ?",
            (rs, rowNum) -> new MapRow(
                rs.getString("agent_id"),
                rs.getString("credential_version"),
                rs.getString("status"),
                rs.getTimestamp("expires_at").toInstant(),
                rs.getTimestamp("last_active_at").toInstant()),
            sessionId);
    }

    private static final class MapRow {
        final String agentId;
        final String credentialVersion;
        final String status;
        final Instant expiresAt;
        final Instant lastActiveAt;

        MapRow(String agentId, String credentialVersion, String status,
               Instant expiresAt, Instant lastActiveAt) {
            this.agentId = agentId;
            this.credentialVersion = credentialVersion;
            this.status = status;
            this.expiresAt = expiresAt;
            this.lastActiveAt = lastActiveAt;
        }
    }
}
