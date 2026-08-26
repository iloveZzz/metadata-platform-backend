package com.yss.datamiddle.aicontextlayer.domain.mcpserver;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 会话生命周期行为测试（契约第 7 节 SEC-05 / 第 9 节 SEC-07）：
 * 会话绑定 Agent 身份与凭据版本；并发会话每 Agent ≤5 → rate_limited；
 * 吊销强制断开联动；会话最大时长按运行策略（REC-05）过期。
 */
class SessionEstablishmentTest {

    private static final Instant NOW = Instant.parse("2026-08-11T08:00:00Z");

    private FakeSessionRepository sessionRepository;
    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionRepository = new FakeSessionRepository();
        sessionManager = new SessionManager(sessionRepository);
    }

    @Test
    void establishesSessionBindsAgentIdentityAndCredentialVersion() {
        McpSession session = sessionManager.establish(activeCredential("agent-1", "v1"), NOW);

        assertThat(session.getSessionId()).isNotBlank();
        assertThat(session.getAgentId()).isEqualTo("agent-1");
        assertThat(session.getCredentialVersion()).isEqualTo("v1");
        assertThat(session.getStatus()).isEqualTo(McpSessionStatus.ACTIVE);
        assertThat(session.isActiveAt(NOW)).isTrue();
    }

    @Test
    void sessionExpiresAfterMaximumDuration() {
        McpSession session = sessionManager.establish(activeCredential("agent-1", "v1"), NOW);

        // 超过会话最大时长（运行策略 REC-05，MVP 默认 30 分钟）后不再活跃
        Instant beyondTtl = NOW.plus(SessionManager.DEFAULT_SESSION_TTL)
            .plus(1, ChronoUnit.MINUTES);
        assertThat(session.isActiveAt(beyondTtl)).isFalse();
    }

    @Test
    void rejectsWhenConcurrencyLimitReached() {
        // 每 Agent 并发会话 ≤5；第 6 个 → rate_limited（契约第 9 节 SEC-07）
        for (int i = 0; i < SessionManager.MAX_CONCURRENT_SESSIONS_PER_AGENT; i++) {
            sessionRepository.save(activeSession("agent-1", "sess-" + i));
        }
        assertThat(sessionRepository.countActiveSessions("agent-1"))
            .isEqualTo(SessionManager.MAX_CONCURRENT_SESSIONS_PER_AGENT);

        assertThatThrownBy(() -> sessionManager.establish(activeCredential("agent-1", "v1"), NOW))
            .isInstanceOf(McpException.class)
            .satisfies(e -> assertThat(((McpException) e).getErrorCode())
                .isEqualTo(McpErrorCode.RATE_LIMITED));
    }

    @Test
    void otherAgentsAreNotLimited() {
        for (int i = 0; i < SessionManager.MAX_CONCURRENT_SESSIONS_PER_AGENT; i++) {
            sessionRepository.save(activeSession("agent-1", "sess-" + i));
        }
        // 另一 Agent 不受限
        McpSession session = sessionManager.establish(activeCredential("agent-2", "v1"), NOW);
        assertThat(session.getAgentId()).isEqualTo("agent-2");
    }

    @Test
    void forceTerminateByCredentialOnlyTerminatesMatchingSessions() {
        sessionRepository.save(activeSession("agent-1", "sess-1", "v2"));
        sessionRepository.save(activeSession("agent-1", "sess-2", "v1"));
        sessionRepository.save(activeSession("agent-2", "sess-3", "v2"));

        int terminated = sessionManager.forceTerminateByCredential("agent-1", "v2");

        assertThat(terminated).isEqualTo(1);
        assertThat(sessionRepository.countActiveSessions("agent-1")).isEqualTo(1);
        assertThat(sessionRepository.countActiveSessions("agent-2")).isEqualTo(1);
        assertThat(sessionRepository.savedSessions().get(0).getStatus())
            .isEqualTo(McpSessionStatus.TERMINATED);
        assertThat(sessionRepository.savedSessions().get(1).getStatus())
            .isEqualTo(McpSessionStatus.ACTIVE);
    }

    @Test
    void reclaimExpiredSessionsMarksExpiredAndFreesQuota() {
        // 回收策略（REC-05）：超过会话最大时长（TTL 30min）的活跃会话被回收为 EXPIRED
        sessionManager.establish(activeCredential("agent-1", "v1"), NOW);
        Instant beyondTtl = NOW.plus(SessionManager.DEFAULT_SESSION_TTL)
            .plus(1, ChronoUnit.MINUTES);

        int reclaimed = sessionManager.reclaimExpiredSessions(beyondTtl);

        assertThat(reclaimed).isEqualTo(1);
        assertThat(sessionRepository.countActiveSessions("agent-1")).isZero();
        assertThat(sessionRepository.savedSessions().get(0).getStatus())
            .isEqualTo(McpSessionStatus.EXPIRED);
        // 过期会话不再占用并发配额：可重新建立
        McpSession newSession = sessionManager.establish(activeCredential("agent-1", "v1"), beyondTtl);
        assertThat(newSession.getStatus()).isEqualTo(McpSessionStatus.ACTIVE);
        // 幂等：重复回收返回 0
        assertThat(sessionManager.reclaimExpiredSessions(beyondTtl)).isZero();
    }

    @Test
    void reclaimExpiredSessionsKeepsNonExpiredSessions() {
        sessionManager.establish(activeCredential("agent-1", "v1"), NOW);
        Instant beforeTtl = NOW.plus(SessionManager.DEFAULT_SESSION_TTL)
            .minus(1, ChronoUnit.MINUTES);

        int reclaimed = sessionManager.reclaimExpiredSessions(beforeTtl);

        assertThat(reclaimed).isZero();
        assertThat(sessionRepository.countActiveSessions("agent-1")).isEqualTo(1);
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

    private static McpSession activeSession(String agentId, String sessionId) {
        return activeSession(agentId, sessionId, "v1");
    }

    private static McpSession activeSession(String agentId, String sessionId, String version) {
        return McpSession.builder()
            .sessionId(sessionId)
            .agentId(agentId)
            .credentialVersion(version)
            .status(McpSessionStatus.ACTIVE)
            .establishedAt(NOW.minus(1, ChronoUnit.MINUTES))
            .lastActiveAt(NOW)
            .expiresAt(NOW.plus(SessionManager.DEFAULT_SESSION_TTL))
            .build();
    }

    /**
     * 测试替身：会话仓库的内存实现（非生产实现）。
     */
    static class FakeSessionRepository implements SessionRepository {
        private final List<McpSession> sessions = new ArrayList<>();
        private final AtomicLong seq = new AtomicLong();

        @Override
        public String nextSessionId() {
            return "sess-" + seq.incrementAndGet();
        }

        @Override
        public int countActiveSessions(String agentId) {
            int count = 0;
            for (McpSession session : sessions) {
                if (session.getAgentId().equals(agentId)
                    && session.getStatus() == McpSessionStatus.ACTIVE) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public void save(McpSession session) {
            sessions.add(session);
        }

        @Override
        public int forceTerminateByCredential(String agentId, String credentialVersion) {
            int terminated = 0;
            for (McpSession session : sessions) {
                if (session.getAgentId().equals(agentId)
                    && session.getCredentialVersion().equals(credentialVersion)
                    && session.getStatus() == McpSessionStatus.ACTIVE) {
                    session.terminate(Instant.now());
                    terminated++;
                }
            }
            return terminated;
        }

        @Override
        public int reclaimExpired(Instant now) {
            int reclaimed = 0;
            for (McpSession session : sessions) {
                if (session.getStatus() == McpSessionStatus.ACTIVE
                    && session.getExpiresAt() != null
                    && !session.getExpiresAt().isAfter(now)) {
                    session.expire();
                    reclaimed++;
                }
            }
            return reclaimed;
        }

        List<McpSession> savedSessions() {
            return sessions;
        }
    }
}
