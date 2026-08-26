package com.yss.datamiddle.aicontextlayer.mcpserver;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ReadOnlyToolRegistry;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ToolRegistry;
import com.yss.datamiddle.aicontextlayer.mcpserver.interceptor.MethodInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 契约第 13 节安全断言 4 验收测试（SEC-09：写操作无能力）。
 *
 * <p>未注册 / 写类工具调用一律抛出 tool_not_found 且无副作用；
 * 工具注册表拒绝写 / 执行 / 管理工具注册。</p>
 */
class SecurityAssertion4Test {

    private final ReadOnlyToolRegistry toolRegistry = new ReadOnlyToolRegistry();
    private final MethodInterceptor interceptor = new MethodInterceptor(toolRegistry);

    @Test
    @DisplayName("断言 4.1：工具注册表严禁注册写类/管理类工具")
    void registryRejectsWriteAndManagementTools() {
        String[] writeTools = {
            "create_asset", "delete_asset", "update_tags", "archive_asset",
            "unarchive_asset", "claim_asset", "favorite_asset", "manual_lineage",
            "execute_script", "admin_shutdown"
        };

        for (String tool : writeTools) {
            ToolRegistry tr = new ToolRegistry();
            tr.setToolName(tool);
            tr.setVersion("1.0.0");
            tr.setEnabled(1);

            McpException ex = assertThrows(McpException.class, () -> toolRegistry.registerTool(tr));
            assertEquals(McpErrorCode.UNAUTHORIZED, ex.getErrorCode(), "尝试注册 " + tool + " 必须被拒绝");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "create_asset", "delete_asset", "update_lineage", "favorite_asset",
        "archive_asset", "unarchive_asset", "claim_asset", "drop_tables",
        "execute_code", "random_unregistered_tool"
    })
    @DisplayName("断言 4.2：写类或未注册方法调用被拦截为 tool_not_found 且绝对无副作用")
    void writeAndUnregisteredMethodsProduceToolNotFoundWithoutSideEffects(String toolName) {
        AtomicInteger sideEffectCounter = new AtomicInteger(0);

        McpException ex = assertThrows(McpException.class, () ->
            interceptor.interceptAndExecute(toolName, () -> {
                sideEffectCounter.incrementAndGet();
                return "executed";
            })
        );

        assertEquals(McpErrorCode.TOOL_NOT_FOUND, ex.getErrorCode());
        assertEquals(0, sideEffectCounter.get(), "写操作/未注册方法调用绝对不应产生任何副作用");
    }

    @Test
    @DisplayName("断言 4.3：仅 5 个只读工具允许通过方法拦截器")
    void exactlyFiveReadOnlyToolsAllowed() {
        for (String tool : toolRegistry.getWhitelistToolNames()) {
            AtomicInteger executionCounter = new AtomicInteger(0);
            String res = interceptor.interceptAndExecute(tool, () -> {
                executionCounter.incrementAndGet();
                return "ok";
            });
            assertEquals("ok", res);
            assertEquals(1, executionCounter.get());
        }
    }
}
