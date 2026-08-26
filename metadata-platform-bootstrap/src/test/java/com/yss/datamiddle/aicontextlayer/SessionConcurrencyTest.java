package com.yss.datamiddle.aicontextlayer;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.AgentCredential;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.CredentialStatus;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 会话并发测试（WU-01-04，DB-backed，替换 InMemory seam）。
 *
 * <p>并发会话每 Agent ≤5（SEC-07，契约第 9 节）：超限 → rate_limited；并发建立会话
 * 均持久化到 mcp_session 表；他 Agent 不受限。</p>
 */
@SpringBootTest(classes = com.yss.metadata.MetadataPlatformApplication.class, properties = {
    "spring.datasource.primary.url=jdbc:h2:mem:ai_context_layer;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.primary.driver-class-name=org.h2.Driver",
    "spring.datasource.primary.username=sa",
    "spring.datasource.primary.password=",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
    "spring.liquibase.enabled=false"
})
class SessionConcurrencyTest {

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
    void rejectsSixthSessionWhenConcurrencyLimitReached() {
        for (int i = 0; i < SessionManager.MAX_CONCURRENT_SESSIONS_PER_AGENT; i++) {
            sessionManager.establish(activeCredential("agent-1", "v1"), NOW);
        }
        assertThat(mcpSessionGateway.countActiveSessions("agent-1"))
            .isEqualTo(SessionManager.MAX_CONCURRENT_SESSIONS_PER_AGENT);

        assertThatThrownBy(() -> sessionManager.establish(activeCredential("agent-1", "v1"), NOW))
            .isInstanceOf(McpException.class)
            .satisfies(e -> assertThat(((McpException) e).getErrorCode())
                .isEqualTo(McpErrorCode.RATE_LIMITED));
        assertThat(mcpSessionGateway.countActiveSessions("agent-1"))
            .isEqualTo(SessionManager.MAX_CONCURRENT_SESSIONS_PER_AGENT);
    }

    @Test
    void otherAgentsAreNotLimited() {
        for (int i = 0; i < SessionManager.MAX_CONCURRENT_SESSIONS_PER_AGENT; i++) {
            sessionManager.establish(activeCredential("agent-1", "v1"), NOW);
        }

        McpSession session = sessionManager.establish(activeCredential("agent-2", "v1"), NOW);

        assertThat(session.getAgentId()).isEqualTo("agent-2");
        assertThat(mcpSessionGateway.countActiveSessions("agent-2")).isEqualTo(1);
    }

    @Test
    void concurrentEstablishForSameAgentPersistsUpToLimit() throws Exception {
        // 并发建立：同一 Agent 并发请求均成功且持久化（每 Agent ≤5 上限内）
        int threads = SessionManager.MAX_CONCURRENT_SESSIONS_PER_AGENT;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<McpSession>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(
                    () -> sessionManager.establish(activeCredential("agent-1", "v1"), NOW)));
            }
            for (Future<McpSession> future : futures) {
                McpSession session = future.get(10, TimeUnit.SECONDS);
                assertThat(session.getStatus()).isEqualTo(McpSessionStatus.ACTIVE);
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(mcpSessionGateway.countActiveSessions("agent-1"))
            .isEqualTo(SessionManager.MAX_CONCURRENT_SESSIONS_PER_AGENT);
        Integer rows = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM mcp_session WHERE agent_id = ? AND status = 'ACTIVE'",
            Integer.class, "agent-1");
        assertThat(rows).isEqualTo(SessionManager.MAX_CONCURRENT_SESSIONS_PER_AGENT);
    }

    @Test
    void concurrentAttemptsBeyondLimitAreAllRateLimited() throws Exception {
        // 配额已满后并发发起：全部 rate_limited，活跃会话数不增长
        for (int i = 0; i < SessionManager.MAX_CONCURRENT_SESSIONS_PER_AGENT; i++) {
            sessionManager.establish(activeCredential("agent-1", "v1"), NOW);
        }
        int attempts = SessionManager.MAX_CONCURRENT_SESSIONS_PER_AGENT + 3;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger rateLimited = new AtomicInteger();
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < attempts; i++) {
                futures.add(pool.submit(() -> {
                    startGate.await();
                    try {
                        sessionManager.establish(activeCredential("agent-1", "v1"), NOW);
                    } catch (McpException e) {
                        if (e.getErrorCode() == McpErrorCode.RATE_LIMITED) {
                            rateLimited.incrementAndGet();
                        }
                    }
                    return null;
                }));
            }
            startGate.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(rateLimited.get()).isEqualTo(attempts);
        assertThat(mcpSessionGateway.countActiveSessions("agent-1"))
            .isEqualTo(SessionManager.MAX_CONCURRENT_SESSIONS_PER_AGENT);
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
}
