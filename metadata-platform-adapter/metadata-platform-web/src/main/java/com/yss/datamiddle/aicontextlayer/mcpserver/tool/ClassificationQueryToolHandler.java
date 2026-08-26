package com.yss.datamiddle.aicontextlayer.mcpserver.tool;

import com.yss.datamiddle.aicontextlayer.application.service.ClassificationToolApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * classification_query MCP 工具处理器（契约 3.5）。
 */
@Component
@RequiredArgsConstructor
public class ClassificationQueryToolHandler implements McpToolHandler {

    public static final String TOOL_NAME = "classification_query";

    private final ClassificationToolApplicationService classificationService;

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Override
    public Object handle(String agentId, String baseUrl, Map<String, Object> arguments) {
        int pageIndex = arguments != null && arguments.get("page") != null ? ((Number) arguments.get("page")).intValue() : 1;
        int pageSize = arguments != null && arguments.get("size") != null ? ((Number) arguments.get("size")).intValue() : 20;

        return classificationService.queryClassifications(agentId, baseUrl, pageIndex, pageSize);
    }
}
