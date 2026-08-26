package com.yss.datamiddle.aicontextlayer.mcpserver.tool;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ReadOnlyToolRegistry;
import com.yss.metadata.application.lineage.service.ColumnImpactAnalysisService;
import com.yss.metadata.client.vo.ColumnImpactAnalysisVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * column_impact_analysis MCP 工具处理器。
 * 支持 LLM Copilot Agent 针对字段变更发起下游爆炸半径评估。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ColumnImpactToolHandler implements McpToolHandler {

    private final ColumnImpactAnalysisService columnImpactAnalysisService;

    @Override
    public String getToolName() {
        return ReadOnlyToolRegistry.TOOL_COLUMN_IMPACT_ANALYSIS;
    }

    @Override
    public Object handle(String agentId, String baseUrl, Map<String, Object> arguments) {
        if (arguments == null || arguments.get("asset_id") == null || arguments.get("column_id") == null) {
            throw McpException.of(McpErrorCode.INVALID_PARAMS);
        }
        String assetId = arguments.get("asset_id").toString();
        String columnId = arguments.get("column_id").toString();
        int maxDepth = 5;
        if (arguments.get("max_depth") != null) {
            try {
                maxDepth = Integer.parseInt(arguments.get("max_depth").toString());
            } catch (NumberFormatException e) {
                log.warn("MCP column_impact_analysis max_depth parameter invalid: {}, fallback to default 5", arguments.get("max_depth"));
            }
        }

        return columnImpactAnalysisService.analyzeImpact(assetId, columnId, maxDepth);
    }
}
