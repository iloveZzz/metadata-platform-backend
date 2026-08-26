package com.yss.datamiddle.aicontextlayer.application.mcpserver;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ConnectionAttempt;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpSession;

/**
 * MCP 连接建立用例（Application 编排边界）。
 *
 * <p>编排「连接鉴权 + 会话建立」；鉴权失败抛 {@link McpException}（由适配层映射
 * 为契约错误响应）；协议版本协商为传输层（适配层）关注点，不在此编排。</p>
 */
public interface McpServerConnectionService {

    /**
     * 建立 MCP 连接：凭据校验 + 吊销检查 + 会话建立（SEC-05）。
     *
     * @param attempt 连接尝试（携带传输期呈现凭据）
     * @return 已建立的会话（绑定 Agent 身份与凭据版本）
     * @throws McpException 凭据缺失 / 无效 / 过期 / 已吊销 → {@code unauthorized}；
     *                      并发会话超限 → {@code rate_limited}
     */
    McpSession establishConnection(ConnectionAttempt attempt);
}
