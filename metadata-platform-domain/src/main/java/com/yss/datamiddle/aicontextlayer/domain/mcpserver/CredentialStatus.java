package com.yss.datamiddle.aicontextlayer.domain.mcpserver;

/**
 * Agent 凭据状态（数据架构 §3/§4）：凭据吊销 / 轮换记录保留用于追溯（status 标记，不物理删除，SEC-05）。
 */
public enum CredentialStatus {

    /** 生效中，可用于连接鉴权。 */
    ACTIVE,
    /** 已吊销，即时失效（含活跃会话强制断开，SEC-05）。 */
    REVOKED,
    /** 已轮换，旧凭据失效。 */
    ROTATED,
    /** 已过期（expires_at 已过）。 */
    EXPIRED
}
