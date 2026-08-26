package com.yss.datamiddle.aicontextlayer.mcpserver.transport;

/**
 * 传输安全违规异常（SEC-11）：明文 Streamable HTTP、凭据出现在查询参数等。
 *
 * <p>消息使用固定常量文案（不含堆栈 / 内部字段名 / 凭据），由适配层映射为
 * {@code invalid_params} 清洁错误响应。</p>
 */
public class McpTransportException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public McpTransportException(String message) {
        super(message);
    }
}
