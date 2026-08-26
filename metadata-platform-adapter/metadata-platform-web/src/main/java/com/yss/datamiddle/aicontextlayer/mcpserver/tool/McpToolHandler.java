package com.yss.datamiddle.aicontextlayer.mcpserver.tool;

import java.util.Map;

/**
 * MCP 只读工具执行处理器接口。
 */
public interface McpToolHandler {

    /**
     * 获取工具名称（如 search_assets、asset_detail）。
     */
    String getToolName();

    /**
     * 执行工具调用。
     *
     * @param agentId Agent 标识
     * @param baseUrl 目标主平台 URL
     * @param arguments 入参字典
     * @return 执行结果对象
     */
    Object handle(String agentId, String baseUrl, Map<String, Object> arguments);
}
