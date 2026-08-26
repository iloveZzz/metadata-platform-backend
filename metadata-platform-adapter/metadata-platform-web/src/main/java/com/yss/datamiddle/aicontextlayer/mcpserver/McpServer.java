package com.yss.datamiddle.aicontextlayer.mcpserver;

import com.yss.datamiddle.aicontextlayer.application.mcpserver.McpServerConnectionService;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ConnectionAttempt;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpProtocolVersion;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpSession;
import com.yss.datamiddle.aicontextlayer.mcpserver.error.ErrorResponse;
import com.yss.datamiddle.aicontextlayer.mcpserver.error.McpErrorMapper;
import com.yss.datamiddle.aicontextlayer.mcpserver.transport.AuthorizationHeaderExtractor;
import com.yss.datamiddle.aicontextlayer.mcpserver.transport.TransportSecurityGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MCP Server 骨架（WU-01-01）—— 连接握手编排的适配层入口。
 *
 * <p>本类为「自定义 MCP 工具处理器适配器」的接入点（BAC B9：无传统 REST Controller，
 * transport 由 MCP SDK / 传输子集提供）。真实 MCP SDK transport（stdio / Streamable HTTP
 * 监听）在 D4 版本锁定后接入；当前 seam 暴露 {@link #handleConnection} 作为连接握手入口。</p>
 *
 * <p>握手流程：传输安全（TLS / 凭据仅 header）→ protocolVersion 协商 →
 * 凭据提取 → 连接鉴权 + 会话建立 → 清洁错误映射（SEC-05 / SEC-09 / SEC-11）。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class McpServer {

    private final McpServerConnectionService connectionService;

    /**
     * 处理 MCP 连接请求。
     *
     * @param request 连接请求（transport 子集 + 请求头 + 协议版本）
     * @return 握手结果：成功返回会话信息，失败返回清洁错误响应（SEC-11）
     */
    public McpConnectionResponse handleConnection(McpConnectionRequest request) {
        // 1. 传输安全（SEC-11）：明文 Streamable HTTP 拒绝；凭据不随查询参数传递
        try {
            if (request != null) {
                TransportSecurityGuard.assertTlsOnly(request.getUrl(), request.getTransportType());
                TransportSecurityGuard.rejectCredentialInQueryParams(request.getQueryParams());
            }
        } catch (RuntimeException e) {
            return McpConnectionResponse.rejected(McpErrorMapper.toErrorResponse(e));
        }

        // 2. protocolVersion 协商（契约第 11 节 SB-01）：缺省回退支持版本，不兼容拒绝
        String requestedVersion = request == null ? null : request.getRequestedProtocolVersion();
        McpProtocolVersion.NegotiationResult negotiation =
            McpProtocolVersion.negotiate(requestedVersion);
        if (!negotiation.isAccepted()) {
            return McpConnectionResponse.rejected(ErrorResponse.of(
                McpErrorCode.INVALID_PARAMS,
                "不支持的 MCP protocolVersion：" + negotiation.getRequested()));
        }

        // 3. 凭据提取（SEC-11）：仅 Authorization header；缺失 → 交由连接鉴权统一拒绝
        //    （SEC-05 禁匿名；SEC-06 鉴权失败留痕由鉴权路径同步写入，WU-01-03）
        String presentedSecret =
            AuthorizationHeaderExtractor.extract(request == null ? null : request.getHeaders()).orElse(null);

        // 4. 连接鉴权 + 会话建立（SEC-05 / SEC-09 硬边界在工具层由 WU05 兜底）
        try {
            McpSession session = connectionService.establishConnection(
                ConnectionAttempt.builder().presentedSecret(presentedSecret).build());
            return McpConnectionResponse.established(session, negotiation.getNegotiated());
        } catch (McpException e) {
            return McpConnectionResponse.rejected(McpErrorMapper.toErrorResponse(e));
        } catch (RuntimeException e) {
            // 未知异常 → internal_error 清洁响应（SEC-11）；日志仅记录异常类型，不含堆栈 / 消息
            // （SEC-11：错误响应与日志不含堆栈、内部字段名、内部配置、凭据）
            log.error("MCP 连接处理发生未预期异常，已映射 internal_error（SEC-11）：{}",
                e.getClass().getName());
            return McpConnectionResponse.rejected(McpErrorMapper.toErrorResponse(e));
        }
    }
}
