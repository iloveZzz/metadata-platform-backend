package com.yss.datamiddle.aicontextlayer.mcpserver.tool;

import com.yss.datamiddle.aicontextlayer.application.service.LineageToolApplicationService;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * lineage MCP 工具处理器（契约 3.3）。
 */
@Component
@RequiredArgsConstructor
public class LineageToolHandler implements McpToolHandler {

    public static final String TOOL_NAME = "lineage";

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
        String confidence = arguments.get("confidence") != null ? arguments.get("confidence").toString() : "all";

        return lineageToolService.getLineage(agentId, baseUrl, assetId, confidence);
    }
}
