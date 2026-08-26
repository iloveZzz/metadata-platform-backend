package com.yss.datamiddle.aicontextlayer.mcpserver.tool;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ReadOnlyToolRegistry;
import com.yss.datamiddle.aicontextlayer.mcpserver.interceptor.MethodInterceptor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 工具请求分发路由器（SEC-09 方法级拦截 + 白名单校验）。
 */
@Component
public class McpToolDispatcher {

    private final MethodInterceptor methodInterceptor;
    private final Map<String, McpToolHandler> handlerMap = new HashMap<>();

    public McpToolDispatcher(MethodInterceptor methodInterceptor, List<McpToolHandler> handlers) {
        this.methodInterceptor = methodInterceptor;
        if (handlers != null) {
            for (McpToolHandler handler : handlers) {
                this.handlerMap.put(handler.getToolName(), handler);
            }
        }
    }

    /**
     * 分发并执行工具调用。
     */
    public Object dispatch(String agentId, String baseUrl, String toolName, Map<String, Object> arguments) {
        // 方法级拦截，非白名单或未注册抛出 TOOL_NOT_FOUND
        methodInterceptor.assertToolAllowed(toolName);

        McpToolHandler handler = handlerMap.get(toolName);
        if (handler == null) {
            throw McpException.of(McpErrorCode.TOOL_NOT_FOUND);
        }

        return handler.handle(agentId, baseUrl, arguments);
    }
}
