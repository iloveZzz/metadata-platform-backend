package com.yss.datamiddle.aicontextlayer.mcpserver.interceptor;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ReadOnlyToolRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodInterceptorTest {

    private final ReadOnlyToolRegistry registry = new ReadOnlyToolRegistry();
    private final MethodInterceptor interceptor = new MethodInterceptor(registry);

    @Test
    @DisplayName("白名单工具通过拦截并正确返回执行结果")
    void allowedToolsPassThrough() {
        String result = interceptor.interceptAndExecute("search_assets", () -> "success-search");
        assertEquals("success-search", result);

        String result2 = interceptor.interceptAndExecute("asset_detail", () -> "success-detail");
        assertEquals("success-detail", result2);
    }

    @Test
    @DisplayName("未注册或写类工具被拦截且执行体不被调用（无副作用）")
    void nonWhitelistToolsAreBlockedWithoutSideEffects() {
        AtomicBoolean executed = new AtomicBoolean(false);

        McpException ex = assertThrows(McpException.class, () ->
            interceptor.interceptAndExecute("create_asset", () -> {
                executed.set(true);
                return "executed";
            })
        );

        assertEquals(McpErrorCode.TOOL_NOT_FOUND, ex.getErrorCode());
        assertFalse(executed.get(), "执行体绝对不应被触发");
    }

    @Test
    @DisplayName("空工具名直接抛出 TOOL_NOT_FOUND")
    void emptyToolNameThrowsToolNotFound() {
        McpException ex1 = assertThrows(McpException.class, () ->
            interceptor.assertToolAllowed(null)
        );
        assertEquals(McpErrorCode.TOOL_NOT_FOUND, ex1.getErrorCode());

        McpException ex2 = assertThrows(McpException.class, () ->
            interceptor.assertToolAllowed("   ")
        );
        assertEquals(McpErrorCode.TOOL_NOT_FOUND, ex2.getErrorCode());
    }
}
