package com.yss.datamiddle.aicontextlayer.domain.mcpserver;

/**
 * MCP 领域异常：携带冻结契约第 10.1 节错误码。
 *
 * <p>对外响应只允许呈现错误码与清洁描述（SEC-11）；堆栈仅用于内部日志，不进入响应。</p>
 */
public class McpException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final McpErrorCode errorCode;

    private McpException(McpErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 构造指定错误码的异常，消息使用错误码冻结描述。
     */
    public static McpException of(McpErrorCode errorCode) {
        return new McpException(errorCode, errorCode.getDescription(), null);
    }

    /**
     * 构造指定错误码的异常并保留底层原因（内部日志使用）。
     */
    public static McpException of(McpErrorCode errorCode, Throwable cause) {
        return new McpException(errorCode, errorCode.getDescription(), cause);
    }

    public McpErrorCode getErrorCode() {
        return errorCode;
    }
}
