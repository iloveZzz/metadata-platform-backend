package com.yss.datamiddle.aicontextlayer.mcpserver.interceptor;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ReadOnlyToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * MCP 工具方法级拦截器（SEC-09 / 安全断言 4）。
 *
 * <p>在工具调用前校验工具名称白名单。未注册工具或写类方法一律阻断并抛出
 * {@link McpErrorCode#TOOL_NOT_FOUND}，确保无任何副作用产生。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MethodInterceptor {

    private final ReadOnlyToolRegistry toolRegistry;

    /**
     * 断言工具允许调用。
     *
     * @param toolName 工具名称
     * @throws McpException 如果工具未注册或不在白名单内
     */
    public void assertToolAllowed(String toolName) {
        if (toolName == null || toolName.trim().isEmpty() || !toolRegistry.isAllowed(toolName)) {
            log.warn("拦截到未注册或非白名单工具调用尝试（SEC-09）：{}", toolName);
            throw McpException.of(McpErrorCode.TOOL_NOT_FOUND);
        }
    }

    /**
     * 拦截并执行工具调用逻辑。
     *
     * @param toolName 工具名称
     * @param execution 工具执行体
     * @param <T> 返回值类型
     * @return 执行结果
     */
    public <T> T interceptAndExecute(String toolName, Supplier<T> execution) {
        assertToolAllowed(toolName);
        return execution.get();
    }
}
