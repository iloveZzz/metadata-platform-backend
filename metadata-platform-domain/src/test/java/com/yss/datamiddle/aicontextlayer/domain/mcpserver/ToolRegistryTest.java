package com.yss.datamiddle.aicontextlayer.domain.mcpserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolRegistryTest {

    private final ReadOnlyToolRegistry registry = new ReadOnlyToolRegistry();

    @Test
    @DisplayName("白名单恰好包含 7 个只读工具（含字段血缘与字段影响分析）")
    void whitelistContainsExactlyFiveReadOnlyTools() {
        assertEquals(7, registry.getWhitelistToolNames().size());
        assertTrue(registry.isAllowed("search_assets"));
        assertTrue(registry.isAllowed("asset_detail"));
        assertTrue(registry.isAllowed("lineage"));
        assertTrue(registry.isAllowed("impact_analysis"));
        assertTrue(registry.isAllowed("classification_query"));
        assertTrue(registry.isAllowed("column_lineage"));
        assertTrue(registry.isAllowed("column_impact_analysis"));
    }

    @Test
    @DisplayName("写类或未注册工具调用被拒绝")
    void rejectsWriteOrUnknownTools() {
        assertFalse(registry.isAllowed("create_asset"));
        assertFalse(registry.isAllowed("delete_asset"));
        assertFalse(registry.isAllowed("update_lineage"));
        assertFalse(registry.isAllowed("execute_sql"));
        assertFalse(registry.isAllowed("unknown_tool"));
        assertFalse(registry.isAllowed(null));
        assertFalse(registry.isAllowed(""));
    }

    @Test
    @DisplayName("注册非白名单工具抛出 UNAUTHORIZED McpException")
    void registerNonWhitelistToolThrowsUnauthorized() {
        ToolRegistry tr = new ToolRegistry();
        tr.setToolName("create_asset");
        tr.setVersion("1.0.0");
        tr.setEnabled(1);

        McpException ex = assertThrows(McpException.class, () -> registry.registerTool(tr));
        assertEquals(McpErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    @DisplayName("注册白名单内工具允许成功")
    void registerWhitelistToolSucceeds() {
        ToolRegistry tr = new ToolRegistry();
        tr.setToolName("search_assets");
        tr.setVersion("1.0.0");
        tr.setEnabled(1);

        registry.registerTool(tr);
        assertTrue(registry.isAllowed("search_assets"));
    }
}
