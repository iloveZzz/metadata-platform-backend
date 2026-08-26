package com.yss.datamiddle.aicontextlayer.domain.mcpserver;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.ToolRegistryGateway;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 只读工具注册表服务（SEC-09，数据架构 §5）。
 *
 * <p>冻结白名单仅包含 5 个只读工具：
 * <ul>
 *   <li>search_assets</li>
 *   <li>asset_detail</li>
 *   <li>lineage</li>
 *   <li>impact_analysis</li>
 *   <li>classification_query</li>
 * </ul>
 * 任何写 / 执行 / 管理工具注册与调用均被严格拒绝。</p>
 */
public class ReadOnlyToolRegistry {

    public static final String TOOL_SEARCH_ASSETS = "search_assets";
    public static final String TOOL_ASSET_DETAIL = "asset_detail";
    public static final String TOOL_LINEAGE = "lineage";
    public static final String TOOL_IMPACT_ANALYSIS = "impact_analysis";
    public static final String TOOL_CLASSIFICATION_QUERY = "classification_query";
    public static final String TOOL_COLUMN_LINEAGE = "column_lineage";
    public static final String TOOL_COLUMN_IMPACT_ANALYSIS = "column_impact_analysis";

    public static final Set<String> WHITELIST_TOOL_NAMES;

    static {
        Set<String> set = new LinkedHashSet<>();
        set.add(TOOL_SEARCH_ASSETS);
        set.add(TOOL_ASSET_DETAIL);
        set.add(TOOL_LINEAGE);
        set.add(TOOL_IMPACT_ANALYSIS);
        set.add(TOOL_CLASSIFICATION_QUERY);
        set.add(TOOL_COLUMN_LINEAGE);
        set.add(TOOL_COLUMN_IMPACT_ANALYSIS);
        WHITELIST_TOOL_NAMES = Collections.unmodifiableSet(set);
    }

    private final ToolRegistryGateway toolRegistryGateway;

    public ReadOnlyToolRegistry() {
        this.toolRegistryGateway = null;
    }

    public ReadOnlyToolRegistry(ToolRegistryGateway toolRegistryGateway) {
        this.toolRegistryGateway = toolRegistryGateway;
    }

    /**
     * 校验工具是否在白名单中且允许调用。
     *
     * @param toolName 工具名称
     * @return 是否允许
     */
    public boolean isAllowed(String toolName) {
        if (toolName == null || toolName.trim().isEmpty()) {
            return false;
        }
        String normalized = toolName.trim();
        if (!WHITELIST_TOOL_NAMES.contains(normalized)) {
            return false;
        }
        if (toolRegistryGateway != null) {
            Optional<ToolRegistry> registryOpt = toolRegistryGateway.getToolRegistryById(normalized);
            if (registryOpt.isPresent()) {
                ToolRegistry reg = registryOpt.get();
                return reg.getEnabled() != null && reg.getEnabled() == 1;
            }
        }
        return true;
    }

    /**
     * 尝试注册工具。若非白名单只读工具或包含写操作语义，抛出异常（SEC-09）。
     *
     * @param toolRegistry 工具元数据
     * @throws McpException 如果尝试注册非白名单工具
     */
    public void registerTool(ToolRegistry toolRegistry) {
        if (toolRegistry == null || toolRegistry.getToolName() == null) {
            throw McpException.of(McpErrorCode.INVALID_PARAMS);
        }
        String toolName = toolRegistry.getToolName().trim();
        if (!WHITELIST_TOOL_NAMES.contains(toolName)) {
            throw McpException.of(McpErrorCode.UNAUTHORIZED);
        }
        if (toolRegistryGateway != null) {
            toolRegistryGateway.addToolRegistry(toolRegistry);
        }
    }

    /**
     * 获取所有白名单工具名称集合。
     */
    public Set<String> getWhitelistToolNames() {
        return WHITELIST_TOOL_NAMES;
    }
}
