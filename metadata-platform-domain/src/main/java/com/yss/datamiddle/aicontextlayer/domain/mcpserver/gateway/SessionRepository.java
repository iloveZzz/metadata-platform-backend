package com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpSession;

import java.time.Instant;

/**
 * MCP 会话仓库端口（Domain 定义，Infrastructure 实现）。
 *
 * <p>持久化约束（数据架构 §5）：mcp_session 单聚合事务写入；吊销强制断开为状态流转
 * 单聚合事务；过期清理按运行策略（REC-05）。</p>
 */
public interface SessionRepository {

    /**
     * 生成新会话 ID。
     */
    String nextSessionId();

    /**
     * 统计某 Agent 当前活跃会话数（并发会话每 Agent ≤5，SEC-07）。
     */
    int countActiveSessions(String agentId);

    /**
     * 保存会话（建立时 append 写入）。
     */
    void save(McpSession session);

    /**
     * 吊销强制断开：终止某 Agent 某凭据版本的所有活跃会话（SEC-05）。
     *
     * @return 实际终止的会话数
     */
    int forceTerminateByCredential(String agentId, String credentialVersion);

    /**
     * 过期 / 空闲回收：将超过会话最大时长（expires_at ≤ now）的活跃会话置为 EXPIRED（REC-05）。
     *
     * <p>MVP 回收策略：TTL 30 分钟（{@link com.yss.datamiddle.aicontextlayer.domain.mcpserver.SessionManager#DEFAULT_SESSION_TTL}），
     * 超过即回收；last_active_at 已持久化，供未来更细粒度空闲回收策略使用。</p>
     *
     * @param now 回收基准时刻
     * @return 实际回收的会话数
     */
    int reclaimExpired(Instant now);
}
