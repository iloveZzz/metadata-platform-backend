package com.yss.datamiddle.aicontextlayer.domain.mcpserver;

import com.yss.cloud.dto.page.PageQuery;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.AuditLogGateway;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.CredentialVerificationGateway;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 连接鉴权行为测试（冻结契约第 7 节 SEC-05）：
 * 凭据缺失 / 无效 / 过期 / 已吊销 → 统一拒绝连接（unauthorized），会话不建立；
 * 吊销即时生效（含活跃会话强制断开）；鉴权失败同步留痕审计（SEC-06，WU-01-03）。
 */
class ConnectionAuthenticatorTest {

    private static final Instant NOW = Instant.parse("2026-08-11T08:00:00Z");

    private FakeCredentialVerificationGateway credentialGateway;
    private FakeSessionRepository sessionRepository;
    private FakeAuditLogGateway auditLogGateway;
    private SessionManager sessionManager;
    private ConnectionAuthenticator authenticator;

    @BeforeEach
    void setUp() {
        credentialGateway = new FakeCredentialVerificationGateway();
        sessionRepository = new FakeSessionRepository();
        auditLogGateway = new FakeAuditLogGateway();
        sessionManager = new SessionManager(sessionRepository);
        authenticator = new ConnectionAuthenticator(credentialGateway, sessionManager, auditLogGateway);
    }

    @Test
    void rejectsMissingCredentialWithoutEstablishingSession() {
        // SEC-05 禁匿名：凭据缺失 → unauthorized，会话不建立，失败留痕（SEC-06）
        assertThatThrownBy(() -> authenticator.authenticate(attempt(null), NOW))
            .isInstanceOf(McpException.class)
            .satisfies(e -> assertThat(((McpException) e).getErrorCode())
                .isEqualTo(McpErrorCode.UNAUTHORIZED));
        assertThat(sessionRepository.savedSessions()).isEmpty();
        assertThat(auditLogGateway.rows()).hasSize(1);
        assertThat(auditLogGateway.rows().get(0).getResultCode()).isEqualTo("unauthorized");
        assertThat(auditLogGateway.rows().get(0).getTool()).isEqualTo("connection");
    }

    @Test
    void rejectsBlankCredentialWithoutEstablishingSession() {
        assertThatThrownBy(() -> authenticator.authenticate(attempt("   "), NOW))
            .isInstanceOf(McpException.class)
            .satisfies(e -> assertThat(((McpException) e).getErrorCode())
                .isEqualTo(McpErrorCode.UNAUTHORIZED));
        assertThat(sessionRepository.savedSessions()).isEmpty();
        assertThat(auditLogGateway.rows()).hasSize(1);
    }

    @Test
    void rejectsInvalidCredentialWithoutEstablishingSession() {
        // 凭据无效（校验失败/不存在）→ unauthorized + 失败留痕
        assertThatThrownBy(() -> authenticator.authenticate(attempt("unknown-secret"), NOW))
            .isInstanceOf(McpException.class)
            .satisfies(e -> assertThat(((McpException) e).getErrorCode())
                .isEqualTo(McpErrorCode.UNAUTHORIZED));
        assertThat(sessionRepository.savedSessions()).isEmpty();
        assertThat(auditLogGateway.rows()).hasSize(1);
    }

    @Test
    void rejectsRevokedCredentialAndForceTerminatesActiveSessions() {
        // 吊销即时生效（SEC-05）：已吊销凭据拒绝连接，且其活跃会话被强制断开
        AgentCredential revoked = AgentCredential.builder()
            .agentId("agent-1")
            .credentialVersion("v2")
            .status(CredentialStatus.REVOKED)
            .issuedAt(NOW.minus(2, ChronoUnit.DAYS))
            .revokedAt(NOW.minus(1, ChronoUnit.HOURS))
            .build();
        credentialGateway.register("revoked-secret", revoked);
        sessionRepository.save(McpSession.builder()
            .sessionId("sess-1")
            .agentId("agent-1")
            .credentialVersion("v2")
            .status(McpSessionStatus.ACTIVE)
            .establishedAt(NOW.minus(1, ChronoUnit.HOURS))
            .lastActiveAt(NOW.minus(10, ChronoUnit.MINUTES))
            .expiresAt(NOW.plus(1, ChronoUnit.HOURS))
            .build());

        assertThatThrownBy(() -> authenticator.authenticate(attempt("revoked-secret"), NOW))
            .isInstanceOf(McpException.class)
            .satisfies(e -> assertThat(((McpException) e).getErrorCode())
                .isEqualTo(McpErrorCode.UNAUTHORIZED));
        // 活跃会话已被强制断开
        assertThat(sessionRepository.countActiveSessions("agent-1")).isZero();
        assertThat(sessionRepository.savedSessions().get(0).getStatus())
            .isEqualTo(McpSessionStatus.TERMINATED);
        // 失败留痕
        assertThat(auditLogGateway.rows()).hasSize(1);
        assertThat(auditLogGateway.rows().get(0).getAgentId()).isEqualTo("agent-1");
    }

    @Test
    void rejectsExpiredCredentialWithoutEstablishingSession() {
        AgentCredential expired = AgentCredential.builder()
            .agentId("agent-1")
            .credentialVersion("v3")
            .status(CredentialStatus.ACTIVE)
            .issuedAt(NOW.minus(30, ChronoUnit.DAYS))
            .expiresAt(NOW.minus(1, ChronoUnit.DAYS))
            .build();
        credentialGateway.register("expired-secret", expired);

        assertThatThrownBy(() -> authenticator.authenticate(attempt("expired-secret"), NOW))
            .isInstanceOf(McpException.class)
            .satisfies(e -> assertThat(((McpException) e).getErrorCode())
                .isEqualTo(McpErrorCode.UNAUTHORIZED));
        assertThat(sessionRepository.savedSessions()).isEmpty();
        assertThat(auditLogGateway.rows()).hasSize(1);
    }

    @Test
    void auditRowDoesNotContainPresentedSecret() {
        // SEC-05/11：审计行不含凭据明文 / 堆栈 / 内部字段名（连接级审计不写参数/结果摘要）
        credentialGateway.register("valid-secret", activeCredential("agent-1", "v1"));
        assertThatThrownBy(() -> authenticator.authenticate(attempt("expired-secret"), NOW))
            .isInstanceOf(McpException.class)
            .satisfies(e -> assertThat(((McpException) e).getErrorCode())
                .isEqualTo(McpErrorCode.UNAUTHORIZED));
        AuditLog audit = auditLogGateway.rows().get(0);
        assertThat(audit.getParamsSummary()).isNull();
        assertThat(audit.getResultSummary()).isNull();
        assertThat(audit.getMcpRequestId()).isNotBlank();
    }

    @Test
    void establishesSessionForValidCredential() {
        AgentCredential valid = activeCredential("agent-1", "v1");
        credentialGateway.register("valid-secret", valid);

        McpSession session = authenticator.authenticate(attempt("valid-secret"), NOW);

        assertThat(session.getAgentId()).isEqualTo("agent-1");
        assertThat(session.getCredentialVersion()).isEqualTo("v1");
        assertThat(session.getStatus()).isEqualTo(McpSessionStatus.ACTIVE);
        assertThat(session.getEstablishedAt()).isEqualTo(NOW);
        assertThat(session.isActiveAt(NOW)).isTrue();
        assertThat(sessionRepository.savedSessions()).hasSize(1);
        // 成功建立会话不写鉴权失败审计
        assertThat(auditLogGateway.rows()).isEmpty();
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

    private static ConnectionAttempt attempt(String presentedSecret) {
        return ConnectionAttempt.builder()
            .presentedSecret(presentedSecret)
            .build();
    }

    /**
     * 测试替身：凭据校验端口的内存实现（非生产实现）。
     */
    static class FakeCredentialVerificationGateway implements CredentialVerificationGateway {
        private final Map<String, AgentCredential> credentialsBySecret = new HashMap<>();

        void register(String secret, AgentCredential credential) {
            credentialsBySecret.put(secret, credential);
        }

        @Override
        public Optional<AgentCredential> verify(String presentedSecret) {
            AgentCredential credential = credentialsBySecret.get(presentedSecret);
            return credential == null ? Optional.empty() : Optional.of(credential);
        }
    }

    /**
     * 测试替身：审计留痕端口的在内存实现（非生产实现）。
     */
    static class FakeAuditLogGateway implements AuditLogGateway {
        private final List<AuditLog> rows = new ArrayList<>();

        @Override
        public String addAuditLog(AuditLog entity) {
            rows.add(entity);
            return entity.getMcpRequestId();
        }

        @Override
        public Optional<AuditLog> getAuditLogById(String id) {
            return rows.stream().filter(r -> r.getMcpRequestId().equals(id)).findFirst();
        }

        @Override
        public PageResult<AuditLog> pageAuditLog(PageQuery query) {
            return PageResult.of(rows, rows.size(), query.getPageSize(), query.getPageIndex());
        }

        List<AuditLog> rows() {
            return rows;
        }
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
