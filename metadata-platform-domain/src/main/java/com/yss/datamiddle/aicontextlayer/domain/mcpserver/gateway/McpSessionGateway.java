package com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway;

import com.yss.cloud.dto.page.PageQuery;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpSession;

import java.time.Instant;
import java.util.Optional;

public interface McpSessionGateway{
    /**
     * Add McpSession
     *
     * @param entity entity
     * @return id
     */
    String addMcpSession(McpSession entity);
    /**
     * Update McpSession
     *
     * @param entity entity
     * @return boolean
     */
    boolean updateMcpSession(McpSession entity);
    /**
     * Delete McpSession
     *
     * @param id id
     * @return boolean
     */
    boolean deleteMcpSession(String id);
    /**
     * Get McpSession by id
     *
     * @param id id
     * @return optional
     */
    Optional<McpSession> getMcpSessionById(String id);
    /**
      * Page McpSession
      *
      * @param query query
      * @return page result
      */
    PageResult<McpSession> pageMcpSession(PageQuery query);

    /**
     * 统计某 Agent 当前活跃（ACTIVE）会话数（并发会话每 Agent ≤5，SEC-07，WU04）。
     */
    int countActiveSessions(String agentId);

    /**
     * 吊销强制断开：将某 Agent 某凭据版本的全部活跃会话终止（SEC-05 吊销即时生效联动，WU04）。
     *
     * <p>单聚合状态流转（ACTIVE → TERMINATED，写 terminated_at），一次 UPDATE 事务完成。</p>
     *
     * @param terminatedAt 终止时间（领域 Instant，存储按 Asia/Shanghai 换算）
     * @return 实际终止的会话数
     */
    int forceTerminateByCredential(String agentId, String credentialVersion, Instant terminatedAt);

    /**
     * 过期 / 空闲回收：将已超过最大时长（expires_at ≤ now）的活跃会话置为 EXPIRED（REC-05，WU04）。
     *
     * <p>单聚合状态流转（ACTIVE → EXPIRED），一次 UPDATE 事务完成；幂等（重复执行返回 0）。</p>
     *
     * @param now 回收基准时刻
     * @return 本次实际回收的会话数
     */
    int reclaimExpired(Instant now);
}
