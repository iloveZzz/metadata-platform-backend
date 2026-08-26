package com.yss.datamiddle.aicontextlayer.mcpserver;

import com.yss.datamiddle.aicontextlayer.mcpserver.transport.McpTransportType;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * MCP transport 连接请求（适配层 seam 输入模型）。
 *
 * <p>凭据不随查询参数 / 工具参数传递（SEC-11）：查询参数存在凭据类键时连接被拒绝；
 * 凭据只允许出现在 {@code headers} 的 Authorization 头。</p>
 */
@Getter
@Builder
public class McpConnectionRequest {

    /** transport 类型（stdio / Streamable HTTP 子集，契约第 11 节 SB-01）。 */
    private final McpTransportType transportType;

    /** Streamable HTTP 目标 URL（TLS 校验用）；stdio 下为 null。 */
    private final String url;

    /** 请求头（仅 Authorization header 承载凭据，SEC-11）。 */
    private final Map<String, String> headers;

    /** 查询参数（出现凭据类键即拒绝，SEC-11）。 */
    private final Map<String, String> queryParams;

    /** 客户端请求的 protocolVersion（缺省回退到支持版本，契约第 11 节）。 */
    private final String requestedProtocolVersion;
}
