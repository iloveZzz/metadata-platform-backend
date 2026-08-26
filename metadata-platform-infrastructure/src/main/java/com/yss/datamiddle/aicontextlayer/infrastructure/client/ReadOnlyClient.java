package com.yss.datamiddle.aicontextlayer.infrastructure.client;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.MetadataPlatformGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 主平台防腐层只读客户端（SEC-01 / SEC-09 / BAC B3）。
 *
 * <p>仅允许消费主平台 5 个只读 GET 端点：
 * <ol>
 *   <li>GET /api/assets (search_assets)</li>
 *   <li>GET /api/assets/{id} (asset_detail)</li>
 *   <li>GET /api/lineage/{id} (lineage)</li>
 *   <li>GET /api/impact/{id} (impact_analysis)</li>
 *   <li>GET /api/classifications (classification_query)</li>
 * </ol>
 * 严禁暴露或调用任何写 API 路径（如 favorite/claim/archive/tags/manual_lineage 等）。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReadOnlyClient implements MetadataPlatformGateway {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(2);

    private final BaseUrlWhitelistValidator baseUrlValidator;
    private final A2GatewayStub a2GatewayStub;

    @Override
    public String searchAssets(String baseUrl, String keyword, int pageIndex, int pageSize) {
        validateTargetUrl(baseUrl);
        log.debug("ReadOnlyClient: 检索资产 keyword={}, pageIndex={}, pageSize={}", keyword, pageIndex, pageSize);
        return "{\"code\":\"200\",\"data\":[],\"totalCount\":0}";
    }

    @Override
    public String getAssetDetail(String baseUrl, String assetId) {
        validateTargetUrl(baseUrl);
        if (assetId == null || assetId.trim().isEmpty()) {
            throw McpException.of(McpErrorCode.INVALID_PARAMS);
        }
        log.debug("ReadOnlyClient: 查询资产详情 assetId={}", assetId);
        return "{\"code\":\"200\",\"data\":{\"id\":\"" + assetId + "\"}}";
    }

    @Override
    public String getLineage(String baseUrl, String assetId, int depth) {
        validateTargetUrl(baseUrl);
        if (assetId == null || assetId.trim().isEmpty()) {
            throw McpException.of(McpErrorCode.INVALID_PARAMS);
        }
        log.debug("ReadOnlyClient: 查询血缘 assetId={}, depth={}", assetId, depth);
        return "{\"code\":\"200\",\"data\":{\"nodes\":[],\"edges\":[]}}";
    }

    @Override
    public String getImpactAnalysis(String baseUrl, String assetId, int depth) {
        validateTargetUrl(baseUrl);
        if (assetId == null || assetId.trim().isEmpty()) {
            throw McpException.of(McpErrorCode.INVALID_PARAMS);
        }
        log.debug("ReadOnlyClient: 影响分析 assetId={}, depth={}", assetId, depth);
        return "{\"code\":\"200\",\"data\":{\"impactedAssets\":[]}}";
    }

    @Override
    public String getClassifications(String baseUrl) {
        validateTargetUrl(baseUrl);
        log.debug("ReadOnlyClient: 查询分级分类");
        return "{\"code\":\"200\",\"data\":[]}";
    }

    public A2GatewayStub getA2GatewayStub() {
        return a2GatewayStub;
    }

    private void validateTargetUrl(String baseUrl) {
        if (baseUrlValidator != null) {
            baseUrlValidator.validate(baseUrl);
        }
    }
}
