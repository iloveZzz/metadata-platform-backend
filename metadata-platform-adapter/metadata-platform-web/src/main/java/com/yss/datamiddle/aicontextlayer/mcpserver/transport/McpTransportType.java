package com.yss.datamiddle.aicontextlayer.mcpserver.transport;

/**
 * MCP transport 子集（冻结契约第 11 节 SB-01）：stdio / Streamable HTTP 子集。
 *
 * <p>Streamable HTTP 仅经 TLS（明文拒绝，SEC-11）；stdio 无 URL 面。</p>
 */
public enum McpTransportType {

    /** stdio transport（进程内标准输入输出）。 */
    STDIO,

    /** Streamable HTTP transport（仅 TLS）。 */
    STREAMABLE_HTTP
}
