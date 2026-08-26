package com.yss.datamiddle.aicontextlayer.mcpserver;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpSession;
import com.yss.datamiddle.aicontextlayer.mcpserver.error.ErrorResponse;
import lombok.Builder;
import lombok.Getter;

/**
 * MCP 连接握手响应（适配层 seam 输出模型）。
 *
 * <p>成功：返回已建立会话信息（sessionId / agentId / credentialVersion）与协商版本；
 * 失败：返回清洁 {@link ErrorResponse}（SEC-11）。</p>
 */
@Getter
@Builder
public class McpConnectionResponse {

    private final boolean established;

    private final String sessionId;

    private final String agentId;

    private final String credentialVersion;

    /** 协商后的 protocolVersion（成功时非 null）。 */
    private final String negotiatedProtocolVersion;

    /** 失败时的清洁错误响应（失败时非 null）。 */
    private final ErrorResponse error;

    public static McpConnectionResponse established(McpSession session, String negotiatedVersion) {
        return McpConnectionResponse.builder()
            .established(true)
            .sessionId(session.getSessionId())
            .agentId(session.getAgentId())
            .credentialVersion(session.getCredentialVersion())
            .negotiatedProtocolVersion(negotiatedVersion)
            .build();
    }

    public static McpConnectionResponse rejected(ErrorResponse error) {
        return McpConnectionResponse.builder()
            .established(false)
            .error(error)
            .build();
    }
}
