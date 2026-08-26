package com.yss.datamiddle.aicontextlayer.infrastructure.mcpserver;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpSession;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.McpSessionGateway;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.SessionRepository;

import java.time.Instant;

/**
 * 会话仓库端口的 DB-backed 实现（WU04，替换 InMemory seam-deferred 实现）。
 *
 * <p>会话持久化到 {@code mcp_session} 表（数据架构 §5）：建立为单聚合插入、吊销强制断开
 * 与过期回收为单聚合状态流转（ACTIVE → TERMINATED / EXPIRED），事务边界在
 * {@link McpSessionGateway} 实现（一次 UPDATE / INSERT 即单聚合事务）。</p>
 *
 * <p>会话 ID 由 MyBatis-Plus 雪花 ID 生成（与 {@code mcp_session.id} 的
 * {@code ASSIGN_ID} 主键策略同源，DDL 注释：表主键 id 即会话 ID）。</p>
 */
public class JdbcSessionRepository implements SessionRepository {

    private final McpSessionGateway mcpSessionGateway;

    public JdbcSessionRepository(McpSessionGateway mcpSessionGateway) {
        this.mcpSessionGateway = mcpSessionGateway;
    }

    @Override
    public String nextSessionId() {
        return IdWorker.getIdStr();
    }

    @Override
    public int countActiveSessions(String agentId) {
        return mcpSessionGateway.countActiveSessions(agentId);
    }

    @Override
    public void save(McpSession session) {
        mcpSessionGateway.addMcpSession(session);
    }

    @Override
    public int forceTerminateByCredential(String agentId, String credentialVersion) {
        return mcpSessionGateway.forceTerminateByCredential(agentId, credentialVersion, Instant.now());
    }

    @Override
    public int reclaimExpired(Instant now) {
        return mcpSessionGateway.reclaimExpired(now);
    }
}
