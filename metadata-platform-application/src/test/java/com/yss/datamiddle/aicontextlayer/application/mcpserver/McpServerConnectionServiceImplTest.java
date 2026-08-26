package com.yss.datamiddle.aicontextlayer.application.mcpserver;

import com.yss.cloud.dto.page.PageQuery;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.aicontextlayer.application.mcpserver.impl.McpServerConnectionServiceImpl;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.AgentCredential;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.AuditLog;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ConnectionAttempt;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ConnectionAuthenticator;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.CredentialStatus;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpSession;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpSessionStatus;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.SessionManager;
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
 * MCP 连接建立用例测试（应用层编排：鉴权 + 会话建立 + 鉴权失败留痕 SEC-06）。
 */
class McpServerConnectionServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-08-11T08:00:00Z");

    private McpServerConnectionServiceImpl service;
    private FakeCredentialVerificationGateway credentialGateway;
    private FakeSessionRepository sessionRepository;
    private FakeAuditLogGateway auditLogGateway;

    @BeforeEach
    void setUp() {
        credentialGateway = new FakeCredentialVerificationGateway();
        sessionRepository = new FakeSessionRepository();
        auditLogGateway = new FakeAuditLogGateway();
        SessionManager sessionManager = new SessionManager(sessionRepository);
        ConnectionAuthenticator authenticator =
            new ConnectionAuthenticator(credentialGateway, sessionManager, auditLogGateway);
        service = new McpServerConnectionServiceImpl(authenticator);
    }

    @Test
    void establishesConnectionForValidCredential() {
        credentialGateway.register("valid-secret", AgentCredential.builder()
            .agentId("agent-1")
            .credentialVersion("v1")
            .status(CredentialStatus.ACTIVE)
            .issuedAt(NOW.minus(1, ChronoUnit.DAYS))
            .expiresAt(NOW.plus(30, ChronoUnit.DAYS))
            .build());

        McpSession session = service.establishConnection(
            ConnectionAttempt.builder().presentedSecret("valid-secret").build());

        assertThat(session.getAgentId()).isEqualTo("agent-1");
        assertThat(session.getStatus()).isEqualTo(McpSessionStatus.ACTIVE);
        assertThat(sessionRepository.savedSessions()).hasSize(1);
        // 成功建立不写鉴权失败审计
        assertThat(auditLogGateway.rows()).isEmpty();
    }

    @Test
    void propagatesUnauthorizedForInvalidCredential() {
        assertThatThrownBy(() -> service.establishConnection(
            ConnectionAttempt.builder().presentedSecret("unknown-secret").build()))
            .isInstanceOf(McpException.class)
            .satisfies(e -> assertThat(((McpException) e).getErrorCode())
                .isEqualTo(McpErrorCode.UNAUTHORIZED));
        assertThat(sessionRepository.savedSessions()).isEmpty();
        // 鉴权失败同步留痕（SEC-06）
        assertThat(auditLogGateway.rows()).hasSize(1);
        assertThat(auditLogGateway.rows().get(0).getResultCode()).isEqualTo("unauthorized");
    }

    @Test
    void writesAuthFailureAuditForMissingCredential() {
        assertThatThrownBy(() -> service.establishConnection(
            ConnectionAttempt.builder().build()))
            .isInstanceOf(McpException.class)
            .satisfies(e -> assertThat(((McpException) e).getErrorCode())
                .isEqualTo(McpErrorCode.UNAUTHORIZED));
        assertThat(sessionRepository.savedSessions()).isEmpty();
        assertThat(auditLogGateway.rows()).hasSize(1);
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
