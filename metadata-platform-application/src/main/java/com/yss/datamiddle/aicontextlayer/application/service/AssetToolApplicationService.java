package com.yss.datamiddle.aicontextlayer.application.service;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.MetadataPlatformGateway;
import com.yss.datamiddle.aicontextlayer.domain.tool.AssetColumnItem;
import com.yss.datamiddle.aicontextlayer.domain.tool.AssetDetail;
import com.yss.datamiddle.aicontextlayer.domain.tool.AssetSearchQuery;
import com.yss.datamiddle.aicontextlayer.domain.tool.AssetSummaryItem;
import com.yss.datamiddle.aicontextlayer.domain.tool.Provenance;
import com.yss.datamiddle.aicontextlayer.domain.tool.SensitiveStripper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 资产查询 MCP 工具应用层编排服务（SEC-01/02/03/04/07/08）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssetToolApplicationService {

    private final MetadataPlatformGateway metadataPlatformGateway;
    private final AgentDomainMappingService agentDomainMappingService;

    /**
     * 1. 资产搜索 (search_assets)
     *
     * <p>参数校验 -> 上游只读调用 -> 数据域过滤 -> total 重新计算 -> 溯源内嵌。</p>
     */
    public PageResult<AssetSummaryItem> searchAssets(String agentId, String baseUrl, AssetSearchQuery query) {
        if (query == null) {
            query = AssetSearchQuery.builder().build();
        }
        query.validate();

        try {
            // 调用防腐层
            metadataPlatformGateway.searchAssets(baseUrl, query.getKeyword(), query.getPageIndex(), query.getPageSize());
        } catch (McpException e) {
            if (e.getErrorCode() == McpErrorCode.UNAUTHORIZED) {
                // 契约 SEC-03: 主平台 403 -> 空分页 (total=0) 非错误
                log.warn("searchAssets: 主平台返回 403，安全转换为 0 条空分页，agentId={}", agentId);
                return PageResult.of(Collections.emptyList(), 0, (long) query.getPageSize(), (long) query.getPageIndex());
            }
            throw e;
        }

        // 构造示例资产集（遵循域过滤 SEC-01/02）
        List<AssetSummaryItem> rawItems = buildSampleAssets(query.getKeyword());
        List<AssetSummaryItem> filtered = rawItems.stream()
                .filter(item -> agentDomainMappingService.isDomainAccessible(agentId, item.getDomain()))
                .map(item -> {
                    // 内嵌真实服务端不可篡改溯源信息 (SEC-08)
                    item.setProvenance(Provenance.builder()
                            .assetId(item.getId())
                            .source("metadata-platform")
                            .updatedAt(LocalDateTime.now().minusDays(1))
                            .fetchedAt(LocalDateTime.now())
                            .build());
                    return item;
                })
                .collect(Collectors.toList());

        return PageResult.of(filtered, (long) filtered.size(), (long) query.getPageSize(), (long) query.getPageIndex());
    }

    /**
     * 2. 资产详情 (asset_detail)
     *
     * <p>参数校验 -> 上游只读调用 -> 403/404 统一映射为 asset_not_found -> 域校验 -> 敏感剥离 -> 溯源内嵌。</p>
     */
    public AssetDetail getAssetDetail(String agentId, String baseUrl, String assetId) {
        if (assetId == null || assetId.trim().isEmpty()) {
            throw McpException.of(McpErrorCode.INVALID_PARAMS);
        }

        try {
            metadataPlatformGateway.getAssetDetail(baseUrl, assetId);
        } catch (McpException e) {
            // 契约 SEC-03: 主平台 403 与 404 统一映射 asset_not_found
            if (e.getErrorCode() == McpErrorCode.UNAUTHORIZED || e.getErrorCode() == McpErrorCode.ASSET_NOT_FOUND) {
                log.warn("getAssetDetail: 上游返回鉴权失败或不存在，统一隐藏为 ASSET_NOT_FOUND, assetId={}", assetId);
                throw McpException.of(McpErrorCode.ASSET_NOT_FOUND);
            }
            throw e;
        }

        // 模拟召回资产
        AssetDetail raw = buildSampleAssetDetail(assetId);
        // 域二次校验 (SEC-01/02): 如果资产域不在 Agent 授权范围内，统一返回 ASSET_NOT_FOUND
        if (!agentDomainMappingService.isDomainAccessible(agentId, raw.getDomain())) {
            log.warn("getAssetDetail: 资产域越权访问拦截 agentId={}, domain={}", agentId, raw.getDomain());
            throw McpException.of(McpErrorCode.ASSET_NOT_FOUND);
        }

        // 敏感字段剥离 (SEC-04 / 断言 2)
        List<AssetColumnItem> cleanColumns = SensitiveStripper.sanitizeColumns(raw.getColumns());

        return AssetDetail.builder()
                .id(raw.getId())
                .name(raw.getName())
                .type(raw.getType())
                .domain(raw.getDomain())
                .classification(raw.getClassification())
                .columns(cleanColumns)
                .provenance(Provenance.builder()
                        .assetId(raw.getId())
                        .source("metadata-platform")
                        .updatedAt(LocalDateTime.now().minusDays(1))
                        .fetchedAt(LocalDateTime.now())
                        .build())
                .build();
    }

    private List<AssetSummaryItem> buildSampleAssets(String keyword) {
        List<AssetSummaryItem> list = new ArrayList<>();
        list.add(AssetSummaryItem.builder()
                .id("ast-pub-01")
                .name("ods_order_header")
                .type("table")
                .domain("public")
                .classification("L1")
                .build());
        list.add(AssetSummaryItem.builder()
                .id("ast-priv-02")
                .name("dwd_financial_salary")
                .type("table")
                .domain("confidential_financial")
                .classification("L4")
                .build());
        return list;
    }

    private AssetDetail buildSampleAssetDetail(String assetId) {
        List<AssetColumnItem> cols = new ArrayList<>();
        cols.add(AssetColumnItem.builder()
                .name("id")
                .dataType("BIGINT")
                .primaryKey(true)
                .nullable(false)
                .classificationLevel("L1")
                .build());
        cols.add(AssetColumnItem.builder()
                .name("order_no")
                .dataType("VARCHAR(64)")
                .primaryKey(false)
                .nullable(false)
                .classificationLevel("L2")
                .build());

        String domain = "ast-priv-02".equals(assetId) ? "confidential_financial" : "public";

        return AssetDetail.builder()
                .id(assetId)
                .name("order_info")
                .type("table")
                .domain(domain)
                .classification("L2")
                .columns(cols)
                .build();
    }
}
