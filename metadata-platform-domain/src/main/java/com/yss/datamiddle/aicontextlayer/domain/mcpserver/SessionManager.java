package com.yss.datamiddle.aicontextlayer.domain.mcpserver;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.SessionRepository;

import java.time.Duration;
import java.time.Instant;

/**
 * 会话生命周期领域服务（契约第 7 节 SEC-05 / 第 9 节 SEC-07）。
 *
 * <p>核心领域规则：会话绑定 Agent 身份与凭据版本；并发会话每 Agent ≤5（超限 rate_limited）；
 * 会话最大时长按运行策略执行（REC-05，MVP 默认 {@link #DEFAULT_SESSION_TTL}）；
 * 吊销强制断开联动（{@link #forceTerminateByCredential}）。</p>
 */
public class SessionManager {

    /** 每 Agent 并发会话上限（契约第 9 节 SEC-07，MVP 默认值，决策 D-07）。 */
    public static final int MAX_CONCURRENT_SESSIONS_PER_AGENT = 5;

    /** 会话最大时长（运行策略 REC-05，MVP 默认 30 分钟；按运行策略校准）。 */
    public static final Duration DEFAULT_SESSION_TTL = Duration.ofMinutes(30);

    private final SessionRepository sessionRepository;

    public SessionManager(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * 建立会话：绑定 Agent 身份与凭据版本，状态 ACTIVE。
     *
     * @param credential 已通过校验且可用的凭据主体
     * @param now        当前时刻（建立 / 过期基准）
     * @return 已持久化的会话
     * @throws McpException 并发会话超过每 Agent 上限时抛出 {@link McpErrorCode#rate_limited}
     */
    public McpSession establish(AgentCredential credential, Instant now) {
        if (sessionRepository.countActiveSessions(credential.getAgentId())
            >= MAX_CONCURRENT_SESSIONS_PER_AGENT) {
            throw McpException.of(McpErrorCode.RATE_LIMITED);
        }
        McpSession session = McpSession.builder()
            .sessionId(sessionRepository.nextSessionId())
            .agentId(credential.getAgentId())
            .credentialVersion(credential.getCredentialVersion())
            .status(McpSessionStatus.ACTIVE)
            .establishedAt(now)
            .lastActiveAt(now)
            .expiresAt(now.plus(DEFAULT_SESSION_TTL))
            .build();
        sessionRepository.save(session);
        return session;
    }

    /**
     * 吊销强制断开：终止某 Agent 某凭据版本的所有活跃会话（SEC-05 吊销即时生效联动）。
     *
     * @return 实际终止的会话数
     */
    public int forceTerminateByCredential(String agentId, String credentialVersion) {
        return sessionRepository.forceTerminateByCredential(agentId, credentialVersion);
    }

    /**
     * 过期 / 空闲回收：将超过会话最大时长（REC-05，MVP 默认 {@link #DEFAULT_SESSION_TTL}）
     * 的活跃会话置为 EXPIRED，避免过期会话继续占用并发配额（并发会话每 Agent ≤5，SEC-07）。
     *
     * <p>回收执行点（调度 / 连接前）由运行部署策略决定；本方法为可重复调用的幂等回收入口，
     * 由 DB 实现执行单聚合状态流转。</p>
     *
     * @param now 回收基准时刻
     * @return 实际回收的会话数
     */
    public int reclaimExpiredSessions(Instant now) {
        return sessionRepository.reclaimExpired(now);
    }
}
