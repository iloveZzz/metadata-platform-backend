package com.yss.datamiddle.aicontextlayer.mcpserver.tool;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ReadOnlyToolRegistry;
import com.yss.metadata.application.lineage.service.ColumnLineageAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * column_lineage MCP 工具处理器。
 * 支持 LLM Agent 查询指定资产的深层字段级血缘图谱。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ColumnLineageToolHandler implements McpToolHandler {

    private final ColumnLineageAppService columnLineageAppService;

    @Override
    public String getToolName() {
        return ReadOnlyToolRegistry.TOOL_COLUMN_LINEAGE;
    }

    @Override
    public Object handle(String agentId, String baseUrl, Map<String, Object> arguments) {
        if (arguments == null || arguments.get("asset_id") == null) {
            throw McpException.of(McpErrorCode.INVALID_PARAMS);
        }
        String assetId = arguments.get("asset_id").toString();
        String columnId = arguments.get("column_id") != null ? arguments.get("column_id").toString() : null;
        int depth = 3;
        if (arguments.get("depth") != null) {
            try {
                depth = Integer.parseInt(arguments.get("depth").toString());
            } catch (NumberFormatException e) {
                log.warn("MCP column_lineage depth parameter invalid: {}, fallback to default 3", arguments.get("depth"));
            }
        }
        String direction = arguments.get("direction") != null ? arguments.get("direction").toString() : "BOTH";

        return columnLineageAppService.getColumnLineageGraph(assetId, columnId, depth, direction);
    }
}
