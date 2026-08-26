package com.yss.datamiddle.aicontextlayer.mcpserver.error;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import lombok.Builder;
import lombok.Getter;

/**
 * MCP 清洁错误响应（SEC-11，契约第 10 节）。
 *
 * <p>对外响应只呈现冻结错误码 + 固定清洁文案 + 可重试标记；不含堆栈、内部字段名、
 * 内部配置、凭据。消息来源仅为 {@link McpErrorCode#getDescription()} 或固定常量文案。</p>
 */
@Getter
@Builder
public class ErrorResponse {

    /** MCP 错误码字面值（契约第 10.1 节）。 */
    private final String code;

    /** 清洁错误消息（不泄漏实现细节，SEC-11）。 */
    private final String message;

    /** 是否可重试（契约「可重试」列）。 */
    private final boolean retryable;

    /**
     * 按错误码构造响应（消息使用错误码冻结描述）。
     */
    public static ErrorResponse of(McpErrorCode errorCode) {
        return ErrorResponse.builder()
            .code(errorCode.getCode())
            .message(errorCode.getDescription())
            .retryable(errorCode.isRetryable())
            .build();
    }

    /**
     * 按错误码构造响应并覆盖消息（仅允许使用固定常量文案，保证响应清洁）。
     */
    public static ErrorResponse of(McpErrorCode errorCode, String message) {
        return ErrorResponse.builder()
            .code(errorCode.getCode())
            .message(message)
            .retryable(errorCode.isRetryable())
            .build();
    }
}
