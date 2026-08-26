package com.yss.datamiddle.aicontextlayer.mcpserver.tool;

import com.yss.datamiddle.aicontextlayer.application.service.AssetToolApplicationService;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * asset_detail 工具处理器（契约 3.2）。
 */
@Component
@RequiredArgsConstructor
public class AssetDetailToolHandler implements McpToolHandler {

    public static final String TOOL_NAME = "asset_detail";

    private final AssetToolApplicationService assetToolService;

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
        return assetToolService.getAssetDetail(agentId, baseUrl, assetId);
    }
}
