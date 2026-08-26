package com.yss.datamiddle.aicontextlayer.domain.mcpserver;

/**
 * MCP 会话状态（数据架构 §4）：活跃 / 已过期 / 已终止（吊销强制断开联动）。
 */
public enum McpSessionStatus {

    /** 活跃会话。 */
    ACTIVE,
    /** 已过期（超出会话最大时长）。 */
    EXPIRED,
    /** 已终止（吊销强制断开 / 显式终止）。 */
    TERMINATED
}
