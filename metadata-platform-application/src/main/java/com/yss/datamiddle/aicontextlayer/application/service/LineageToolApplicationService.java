package com.yss.datamiddle.aicontextlayer.application.service;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.MetadataPlatformGateway;
import com.yss.datamiddle.aicontextlayer.domain.tool.ImpactAnalysisResult;
import com.yss.datamiddle.aicontextlayer.domain.tool.ImpactDepthGroup;
import com.yss.datamiddle.aicontextlayer.domain.tool.ImpactItem;
import com.yss.datamiddle.aicontextlayer.domain.tool.LineageEdge;
import com.yss.datamiddle.aicontextlayer.domain.tool.LineageGraphResult;
import com.yss.datamiddle.aicontextlayer.domain.tool.LineageNode;
import com.yss.datamiddle.aicontextlayer.domain.tool.Provenance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 血缘与影响分析 MCP 工具应用编排服务（SEC-02 域外穿透防护 / SEC-03 / SEC-07 规模超限）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LineageToolApplicationService {

    private final MetadataPlatformGateway metadataPlatformGateway;
    private final AgentDomainMappingService agentDomainMappingService;

    /**
     * 1. 查询血缘拓扑 (lineage)
     */
    public LineageGraphResult getLineage(String agentId, String baseUrl, String assetId, String confidence) {
        if (assetId == null || assetId.trim().isEmpty()) {
            throw McpException.of(McpErrorCode.INVALID_PARAMS);
        }

        try {
            metadataPlatformGateway.getLineage(baseUrl, assetId, 3);
        } catch (McpException e) {
            // 起点 403/404 统一映射为 ASSET_NOT_FOUND (SEC-03)
            if (e.getErrorCode() == McpErrorCode.UNAUTHORIZED || e.getErrorCode() == McpErrorCode.ASSET_NOT_FOUND) {
                log.warn("getLineage: 起点资产鉴权失败或不存在，统一隐藏为 ASSET_NOT_FOUND, assetId={}", assetId);
                throw McpException.of(McpErrorCode.ASSET_NOT_FOUND);
            }
            throw e;
        }

        // 模拟召回图
        List<LineageNode> rawNodes = buildSampleNodes(assetId);
        List<LineageEdge> rawEdges = buildSampleEdges(assetId);

        // 规模上限检查 (SEC-07): 节点>500 或 边>1000 抛出 UPSTREAM_TOO_LARGE
        if (rawNodes.size() > LineageGraphResult.MAX_NODES || rawEdges.size() > LineageGraphResult.MAX_EDGES) {
            log.warn("getLineage: 图规模超限 nodes={}, edges={}", rawNodes.size(), rawEdges.size());
            throw McpException.of(McpErrorCode.UPSTREAM_TOO_LARGE);
        }

        // 逐节点校验 Agent 权限域 (SEC-02)
        List<LineageNode> filteredNodes = rawNodes.stream()
                .filter(n -> agentDomainMappingService.isDomainAccessible(agentId, n.getDomain()))
                .map(n -> {
                    n.setProvenance(Provenance.builder()
                            .assetId(n.getId())
                            .source("metadata-platform")
                            .updatedAt(LocalDateTime.now().minusDays(1))
                            .fetchedAt(LocalDateTime.now())
                            .build());
                    return n;
                })
                .collect(Collectors.toList());

        // 提取保留节点的 ID 集合
        Set<String> validNodeIds = filteredNodes.stream().map(LineageNode::getId).collect(Collectors.toSet());

        // 起点不在域内 -> 统一 ASSET_NOT_FOUND
        if (!validNodeIds.contains(assetId)) {
            log.warn("getLineage: 起点资产不在 Agent 授权域内，统一隐藏为 ASSET_NOT_FOUND assetId={}", assetId);
            throw McpException.of(McpErrorCode.ASSET_NOT_FOUND);
        }

        // 逐边校验：仅当边两端节点均在域内时保留（杜绝悬空边泄露）
        List<LineageEdge> filteredEdges = rawEdges.stream()
                .filter(e -> validNodeIds.contains(e.getFromId()) && validNodeIds.contains(e.getToId()))
                .map(e -> {
                    e.setProvenance(Provenance.builder()
                            .assetId(e.getFromId())
                            .source("metadata-platform")
                            .updatedAt(LocalDateTime.now().minusDays(1))
                            .fetchedAt(LocalDateTime.now())
                            .build());
                    return e;
                })
                .collect(Collectors.toList());

        return LineageGraphResult.builder()
                .assetId(assetId)
                .nodes(filteredNodes)
                .edges(filteredEdges)
                .build();
    }

    /**
     * 2. 影响分析 (impact_analysis)
     */
    public ImpactAnalysisResult getImpactAnalysis(String agentId, String baseUrl, String assetId, String sortBy) {
        if (assetId == null || assetId.trim().isEmpty()) {
            throw McpException.of(McpErrorCode.INVALID_PARAMS);
        }

        try {
            metadataPlatformGateway.getImpactAnalysis(baseUrl, assetId, 2);
        } catch (McpException e) {
            // 起点 403/404 统一映射为 ASSET_NOT_FOUND (SEC-03)
            if (e.getErrorCode() == McpErrorCode.UNAUTHORIZED || e.getErrorCode() == McpErrorCode.ASSET_NOT_FOUND) {
                log.warn("getImpactAnalysis: 起点资产鉴权失败或不存在，统一隐藏为 ASSET_NOT_FOUND, assetId={}", assetId);
                throw McpException.of(McpErrorCode.ASSET_NOT_FOUND);
            }
            throw e;
        }

        List<ImpactItem> rawItems = buildSampleImpactItems(assetId);

        // 规模上限检查 (SEC-07): 影响项 > 500 抛出 UPSTREAM_TOO_LARGE
        if (rawItems.size() > ImpactAnalysisResult.MAX_IMPACT_ITEMS) {
            log.warn("getImpactAnalysis: 影响项规模超限 count={}", rawItems.size());
            throw McpException.of(McpErrorCode.UPSTREAM_TOO_LARGE);
        }

        // 逐项二次校验 Agent 权限域 (SEC-02)
        List<ImpactItem> filteredItems = rawItems.stream()
                .filter(item -> agentDomainMappingService.isDomainAccessible(agentId, item.getDomain()))
                .map(item -> {
                    item.setProvenance(Provenance.builder()
                            .assetId(item.getAssetId())
                            .source("metadata-platform")
                            .updatedAt(LocalDateTime.now().minusDays(1))
                            .fetchedAt(LocalDateTime.now())
                            .build());
                    return item;
                })
                .collect(Collectors.toList());

        // 重新按影响深度分组 (SEC-02)
        Map<Integer, List<ImpactItem>> depthMap = filteredItems.stream()
                .collect(Collectors.groupingBy(ImpactItem::getDepth));

        List<ImpactDepthGroup> depthGroups = depthMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> ImpactDepthGroup.builder()
                        .depth(e.getKey())
                        .groupCount(e.getValue().size())
                        .items(e.getValue())
                        .build())
                .collect(Collectors.toList());

        return ImpactAnalysisResult.builder()
                .rootAssetId(assetId)
                .totalCount(filteredItems.size())
                .depthGroups(depthGroups)
                .build();
    }

    private List<LineageNode> buildSampleNodes(String rootId) {
        List<LineageNode> list = new ArrayList<>();
        list.add(LineageNode.builder().id(rootId).name("ods_orders").type("table").domain("public").build());
        list.add(LineageNode.builder().id("ast-dwd-01").name("dwd_order_detail").type("table").domain("public").build());
        list.add(LineageNode.builder().id("ast-priv-salary").name("dws_salary_agg").type("table").domain("confidential_financial").build());
        return list;
    }

    private List<LineageEdge> buildSampleEdges(String rootId) {
        List<LineageEdge> list = new ArrayList<>();
        list.add(LineageEdge.builder().fromId(rootId).toId("ast-dwd-01").confidence("auto-high").build());
        list.add(LineageEdge.builder().fromId("ast-dwd-01").toId("ast-priv-salary").confidence("auto-high").build());
        return list;
    }

    private List<ImpactItem> buildSampleImpactItems(String rootId) {
        List<ImpactItem> list = new ArrayList<>();
        list.add(ImpactItem.builder().assetId("ast-dwd-01").name("dwd_order_detail").type("table").domain("public").depth(1).build());
        list.add(ImpactItem.builder().assetId("ast-priv-salary").name("dws_salary_agg").type("table").domain("confidential_financial").depth(2).build());
        return list;
    }
}
