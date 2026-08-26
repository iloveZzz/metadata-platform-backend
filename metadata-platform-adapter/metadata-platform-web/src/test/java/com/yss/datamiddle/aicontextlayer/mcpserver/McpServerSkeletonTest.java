package com.yss.datamiddle.aicontextlayer.mcpserver;

import com.yss.cloud.dto.page.PageQuery;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.aicontextlayer.application.mcpserver.McpServerConnectionService;
import com.yss.datamiddle.aicontextlayer.application.mcpserver.impl.McpServerConnectionServiceImpl;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.AgentCredential;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.AuditLog;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ConnectionAttempt;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ConnectionAuthenticator;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.CredentialStatus;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpSession;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpSessionStatus;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.SessionManager;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.AuditLogGateway;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.CredentialVerificationGateway;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.SessionRepository;
import com.yss.datamiddle.aicontextlayer.mcpserver.transport.McpTransportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MCP Server 骨架握手行为测试（WU-01-01；WU-01-03 追加鉴权失败留痕接线断言）：
 * transport 子集对齐（协议版本协商 / TLS / 凭据仅 header）→ 连接鉴权 → 会话建立；
 * 未授权连接统一 unauthorized；未知异常 internal_error 且响应清洁（SEC-11）。
 */
class McpServerSkeletonTest {

    private static final Instant NOW = Instant.parse("2026-08-11T08:00:00Z");

    private McpServer server;
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
        McpServerConnectionService connectionService =
            new McpServerConnectionServiceImpl(authenticator);
        server = new McpServer(connectionService);
    }

    @Test
    void establishesSessionForValidCredentialOverHttps() {
        credentialGateway.register("valid-secret", activeCredential("agent-1", "v1"));

        McpConnectionResponse response = server.handleConnection(
            McpConnectionRequest.builder()
                .transportType(McpTransportType.STREAMABLE_HTTP)
                .url("https://metadata-platform.example")
                .headers(headers("Bearer valid-secret"))
                .requestedProtocolVersion("2025-06-18")
                .build());

        assertThat(response.isEstablished()).isTrue();
        assertThat(response.getAgentId()).isEqualTo("agent-1");
        assertThat(response.getNegotiatedProtocolVersion()).isEqualTo("2025-06-18");
        assertThat(response.getError()).isNull();
        assertThat(sessionRepository.savedSessions()).hasSize(1);
    }

    @Test
    void rejectsConnectionWithoutCredentialAsUnauthorized() {
        McpConnectionResponse response = server.handleConnection(
            McpConnectionRequest.builder()
                .transportType(McpTransportType.STREAMABLE_HTTP)
                .url("https://metadata-platform.example")
                .headers(Collections.emptyMap())
                .build());

        assertThat(response.isEstablished()).isFalse();
        assertThat(response.getError().getCode()).isEqualTo("unauthorized");
        assertThat(sessionRepository.savedSessions()).isEmpty();
        // 凭据缺失也同步留痕（SEC-06，WU-01-03）
        assertThat(auditLogGateway.rows()).hasSize(1);
    }

    @Test
    void rejectsInvalidCredentialAsUnauthorized() {
        McpConnectionResponse response = server.handleConnection(
            McpConnectionRequest.builder()
                .transportType(McpTransportType.STREAMABLE_HTTP)
                .url("https://metadata-platform.example")
                .headers(headers("Bearer wrong-secret"))
                .build());

        assertThat(response.isEstablished()).isFalse();
        assertThat(response.getError().getCode()).isEqualTo("unauthorized");
        assertThat(sessionRepository.savedSessions()).isEmpty();
        assertThat(auditLogGateway.rows()).hasSize(1);
    }

    @Test
    void rejectsRevokedCredentialAsUnauthorizedAndForceTerminatesSessions() {
        credentialGateway.register("revoked-secret", AgentCredential.builder()
            .agentId("agent-1").credentialVersion("v2").status(CredentialStatus.REVOKED)
            .issuedAt(NOW.minus(2, ChronoUnit.DAYS)).revokedAt(NOW.minus(1, ChronoUnit.HOURS)).build());
        sessionRepository.save(activeSession("agent-1", "sess-1", "v2"));

        McpConnectionResponse response = server.handleConnection(
            McpConnectionRequest.builder()
                .transportType(McpTransportType.STREAMABLE_HTTP)
                .url("https://metadata-platform.example")
                .headers(headers("Bearer revoked-secret"))
                .build());

        assertThat(response.isEstablished()).isFalse();
        assertThat(response.getError().getCode()).isEqualTo("unauthorized");
        assertThat(sessionRepository.countActiveSessions("agent-1")).isZero();
    }

    @Test
    void rejectsUnsupportedProtocolVersion() {
        McpConnectionResponse response = server.handleConnection(
            McpConnectionRequest.builder()
                .transportType(McpTransportType.STREAMABLE_HTTP)
                .url("https://metadata-platform.example")
                .headers(headers("Bearer valid-secret"))
                .requestedProtocolVersion("2030-01-01")
                .build());

        assertThat(response.isEstablished()).isFalse();
        assertThat(response.getError().getCode()).isEqualTo("invalid_params");
        assertThat(sessionRepository.savedSessions()).isEmpty();
    }

    @Test
    void rejectsPlainHttpTransport() {
        // SEC-11：明文 Streamable HTTP 拒绝
        McpConnectionResponse response = server.handleConnection(
            McpConnectionRequest.builder()
                .transportType(McpTransportType.STREAMABLE_HTTP)
                .url("http://metadata-platform.example")
                .headers(headers("Bearer valid-secret"))
                .build());

        assertThat(response.isEstablished()).isFalse();
        assertThat(response.getError().getCode()).isEqualTo("invalid_params");
        assertThat(response.getError().getMessage()).contains("TLS");
        assertThat(sessionRepository.savedSessions()).isEmpty();
    }

    @Test
    void rejectsCredentialInQueryParams() {
        // SEC-11：凭据不随查询参数传递
        McpConnectionResponse response = server.handleConnection(
            McpConnectionRequest.builder()
                .transportType(McpTransportType.STREAMABLE_HTTP)
                .url("https://metadata-platform.example")
                .headers(headers("Bearer valid-secret"))
                .queryParams(Collections.singletonMap("token", "should-not-be-used"))
                .build());

        assertThat(response.isEstablished()).isFalse();
        assertThat(response.getError().getCode()).isEqualTo("invalid_params");
        assertThat(sessionRepository.savedSessions()).isEmpty();
    }

    @Test
    void mapsUnexpectedFailureToCleanInternalError() {
        McpServer failingServer = new McpServer(new McpServerConnectionService() {
            @Override
            public McpSession establishConnection(ConnectionAttempt attempt) {
                throw new IllegalStateException("presentedSecret=leak at com.yss.internal.SecretService");
            }
        });

        McpConnectionResponse response = failingServer.handleConnection(
            McpConnectionRequest.builder()
                .transportType(McpTransportType.STREAMABLE_HTTP)
                .url("https://metadata-platform.example")
                .headers(headers("Bearer valid-secret"))
                .build());

        assertThat(response.isEstablished()).isFalse();
        assertThat(response.getError().getCode()).isEqualTo("internal_error");
        String message = response.getError().getMessage().toLowerCase();
        assertThat(message).doesNotContain("leak");
        assertThat(message).doesNotContain("com.yss.internal");
        assertThat(message).doesNotContain("stacktrace");
    }

    @Test
    void stdioTransportSkipsUrlCheck() {
        credentialGateway.register("valid-secret", activeCredential("agent-1", "v1"));

        McpConnectionResponse response = server.handleConnection(
            McpConnectionRequest.builder()
                .transportType(McpTransportType.STDIO)
                .headers(headers("Bearer valid-secret"))
                .build());

        assertThat(response.isEstablished()).isTrue();
    }

    private static AgentCredential activeCredential(String agentId, String version) {
        return AgentCredential.builder()
            .agentId(agentId).credentialVersion(version).status(CredentialStatus.ACTIVE)
            .issuedAt(NOW.minus(1, ChronoUnit.DAYS)).expiresAt(NOW.plus(30, ChronoUnit.DAYS))
            .build();
    }

    private static McpSession activeSession(String agentId, String sessionId, String version) {
        return McpSession.builder()
            .sessionId(sessionId).agentId(agentId).credentialVersion(version)
            .status(McpSessionStatus.ACTIVE)
            .establishedAt(NOW.minus(1, ChronoUnit.MINUTES))
            .lastActiveAt(NOW)
            .expiresAt(NOW.plus(SessionManager.DEFAULT_SESSION_TTL))
            .build();
    }

    private static Map<String, String> headers(String authorization) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", authorization);
        return headers;
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
