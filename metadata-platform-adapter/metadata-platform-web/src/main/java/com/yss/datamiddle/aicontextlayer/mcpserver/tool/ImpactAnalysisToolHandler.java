package com.yss.datamiddle.aicontextlayer.mcpserver.tool;

import com.yss.datamiddle.aicontextlayer.application.service.LineageToolApplicationService;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * impact_analysis MCP 工具处理器（契约 3.4）。
 */
@Component
@RequiredArgsConstructor
public class ImpactAnalysisToolHandler implements McpToolHandler {

    public static final String TOOL_NAME = "impact_analysis";

    private final LineageToolApplicationService lineageToolService;

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Override
    public Object handle(String agentId, String baseUrl, Map<String, Object> arguments) {
        if (arguments == null || arguments.get("asset_id") == null) {
            throw McpException.of(McpErrorCode.INVALID_PARAMS);
        }
        String assetId = arguments.get("asset_id").toString();
        String sortBy = arguments.get("sort_by") != null ? arguments.get("sort_by").toString() : "depth";

        return lineageToolService.getImpactAnalysis(agentId, baseUrl, assetId, sortBy);
    }
}
