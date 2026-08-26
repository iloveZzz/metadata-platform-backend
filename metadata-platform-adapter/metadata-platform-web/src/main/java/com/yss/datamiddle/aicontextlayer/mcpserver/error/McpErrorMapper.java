package com.yss.datamiddle.aicontextlayer.mcpserver.error;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import com.yss.datamiddle.aicontextlayer.mcpserver.transport.McpTransportException;

/**
 * 异常 → MCP 错误码映射（契约第 10.1 节）。
 *
 * <p>映射规则：领域异常按自身错误码；传输安全违规（SEC-11）映射 {@code invalid_params}
 * （固定文案）；未知异常统一映射 {@code internal_error}（清洁响应，不含堆栈 / 内部字段名）。</p>
 */
public final class McpErrorMapper {

    private McpErrorMapper() {
    }

    /**
     * 将异常映射为清洁错误响应。
     *
     * @param throwable 待映射异常（null 按未知异常处理）
     * @return 清洁错误响应（SEC-11）
     */
    public static ErrorResponse toErrorResponse(Throwable throwable) {
        if (throwable instanceof McpException) {
            return ErrorResponse.of(((McpException) throwable).getErrorCode());
        }
        if (throwable instanceof McpTransportException) {
            return ErrorResponse.of(McpErrorCode.INVALID_PARAMS, throwable.getMessage());
        }
        // 未知异常 → internal_error：消息使用错误码冻结描述，异常详情只进内部日志（SEC-11）
        return ErrorResponse.of(McpErrorCode.INTERNAL_ERROR);
    }
}
