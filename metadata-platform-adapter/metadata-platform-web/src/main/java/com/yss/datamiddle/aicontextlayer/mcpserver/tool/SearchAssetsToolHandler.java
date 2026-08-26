package com.yss.datamiddle.aicontextlayer.mcpserver.tool;

import com.yss.datamiddle.aicontextlayer.application.service.AssetToolApplicationService;
import com.yss.datamiddle.aicontextlayer.domain.tool.AssetSearchQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * search_assets 工具处理器（契约 3.1）。
 */
@Component
@RequiredArgsConstructor
public class SearchAssetsToolHandler implements McpToolHandler {

    public static final String TOOL_NAME = "search_assets";

    private final AssetToolApplicationService assetToolService;

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Override
    public Object handle(String agentId, String baseUrl, Map<String, Object> arguments) {
        String keyword = arguments != null && arguments.get("keyword") != null ? arguments.get("keyword").toString() : null;
        int pageIndex = arguments != null && arguments.get("page") != null ? ((Number) arguments.get("page")).intValue() : 1;
        int pageSize = arguments != null && arguments.get("size") != null ? ((Number) arguments.get("size")).intValue() : 20;

        AssetSearchQuery query = AssetSearchQuery.builder()
                .keyword(keyword)
                .pageIndex(pageIndex)
                .pageSize(pageSize)
                .build();

        return assetToolService.searchAssets(agentId, baseUrl, query);
    }
}
