package com.yss.datamiddle.aicontextlayer.domain.mcpserver;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.Instant;

/**
 * MCP 会话实体（数据架构 §3/§4，契约第 7 节 SEC-05）。
 *
 * <p>会话绑定 Agent 身份与凭据版本；吊销即时失效（活跃会话强制断开）；会话空闲回收与
 * 最大时长按运行策略执行（REC-05，MVP 默认 30 分钟）。</p>
 *
 * <p>实现 {@link Serializable}：作为持久层 Gateway 分页返回类型（{@code PageResult<T extends Serializable>}）要求。</p>
 */
@Getter
@Builder
public class McpSession implements Serializable {

    private static final long serialVersionUID = 1L;


    private final String sessionId;

    private final String agentId;

    /** 会话绑定的凭据版本（凭据吊销即时失效联动）。 */
    private final String credentialVersion;

    private McpSessionStatus status;

    private final Instant establishedAt;

    private Instant lastActiveAt;

    private final Instant expiresAt;

    private Instant terminatedAt;

    /**
     * 会话在指定时刻是否活跃（状态 ACTIVE 且未超过最大时长）。
     */
    public boolean isActiveAt(Instant now) {
        return status == McpSessionStatus.ACTIVE && !expiresAt.isBefore(now);
    }

    /**
     * 终止会话（吊销强制断开 / 显式终止）。
     *
     * @param now 终止时间
     */
    public void terminate(Instant now) {
        this.status = McpSessionStatus.TERMINATED;
        this.terminatedAt = now;
    }

    /**
     * 标记会话已过期（超时 / 空闲回收，REC-05）：ACTIVE → EXPIRED。
     *
     * <p>与终止（吊销 / 显式）语义区分：过期是时间驱动的状态流转，
     * 不写 terminated_at（数据架构 §4：活跃 / 已过期 / 已终止 三态）。</p>
     */
    public void expire() {
        this.status = McpSessionStatus.EXPIRED;
    }

    /**
     * 更新最近活跃时间（空闲回收依据，REC-05）。
     */
    public void touch(Instant now) {
        this.lastActiveAt = now;
    }
}
