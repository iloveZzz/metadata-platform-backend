package com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway;

/**
 * 主平台防腐层只读网关端口（SEC-01 / SEC-09）。
 */
public interface MetadataPlatformGateway {
    String searchAssets(String baseUrl, String keyword, int pageIndex, int pageSize);
    String getAssetDetail(String baseUrl, String assetId);
    String getLineage(String baseUrl, String assetId, int depth);
    String getImpactAnalysis(String baseUrl, String assetId, int depth);
    String getClassifications(String baseUrl);
}
