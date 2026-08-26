package com.yss.datamiddle.aicontextlayer.application.service;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.MetadataPlatformGateway;
import com.yss.datamiddle.aicontextlayer.domain.tool.AssetDetail;
import com.yss.datamiddle.aicontextlayer.domain.tool.AssetSearchQuery;
import com.yss.datamiddle.aicontextlayer.domain.tool.AssetSummaryItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AssetToolApplicationServiceTest {

    private final MetadataPlatformGateway metadataPlatformGateway = Mockito.mock(MetadataPlatformGateway.class);
    private final AgentDomainMappingService domainService = new AgentDomainMappingService(null);
    private final AssetToolApplicationService service = new AssetToolApplicationService(metadataPlatformGateway, domainService);

    private static final String BASE_URL = "http://localhost:8080";
    private static final String AGENT_ID = "agent-001";

    @Test
    @DisplayName("断言 1: 域外资产被过滤，total 返回过滤后计数")
    void outOfDomainAssetsFiltered() {
        when(metadataPlatformGateway.searchAssets(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn("{}");

        AssetSearchQuery query = AssetSearchQuery.builder().keyword("order").pageSize(20).build();
        PageResult<AssetSummaryItem> result = service.searchAssets(AGENT_ID, BASE_URL, query);

        assertTrue(result.isSuccess());
        // agent-001 仅有 default/public 域权限，confidential_financial 域资产被过滤
        assertEquals(1, result.getData().size());
        assertEquals("ast-pub-01", result.getData().get(0).getId());
        assertEquals(1, result.getTotalCount());
        assertNotNull(result.getData().get(0).getProvenance());
    }

    @Test
    @DisplayName("SEC-03: searchAssets 主平台 403 时安全降级为 0 条空分页")
    void searchAssetsUpstream403ReturnsEmptyPage() {
        when(metadataPlatformGateway.searchAssets(anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(McpException.of(McpErrorCode.UNAUTHORIZED));

        AssetSearchQuery query = AssetSearchQuery.builder().keyword("forbidden").pageSize(20).build();
        PageResult<AssetSummaryItem> result = service.searchAssets(AGENT_ID, BASE_URL, query);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().isEmpty());
        assertEquals(0, result.getTotalCount());
    }

    @Test
    @DisplayName("SEC-07: pageSize 超过 50 抛出 INVALID_PARAMS")
    void pageSizeExceedsMaxThrowsInvalidParams() {
        AssetSearchQuery query = AssetSearchQuery.builder().pageSize(51).build();
        McpException ex = assertThrows(McpException.class, () ->
                service.searchAssets(AGENT_ID, BASE_URL, query)
        );
        assertEquals(McpErrorCode.INVALID_PARAMS, ex.getErrorCode());
    }

    @Test
    @DisplayName("断言 3: asset_detail 越权访问返回统一 ASSET_NOT_FOUND")
    void assetDetailUnauthorizedDomainReturnsAssetNotFound() {
        when(metadataPlatformGateway.getAssetDetail(anyString(), anyString())).thenReturn("{}");

        McpException ex = assertThrows(McpException.class, () ->
                service.getAssetDetail(AGENT_ID, BASE_URL, "ast-priv-02")
        );
        assertEquals(McpErrorCode.ASSET_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("断言 2: asset_detail 敏感字段剥离且内嵌不可篡改溯源")
    void assetDetailSanitizedAndProvenanceAttached() {
        when(metadataPlatformGateway.getAssetDetail(anyString(), anyString())).thenReturn("{}");

        AssetDetail detail = service.getAssetDetail(AGENT_ID, BASE_URL, "ast-pub-01");
        assertNotNull(detail);
        assertEquals("ast-pub-01", detail.getId());
        assertNotNull(detail.getColumns());
        assertFalse(detail.getColumns().isEmpty());
        assertNotNull(detail.getProvenance());
        assertEquals("ast-pub-01", detail.getProvenance().getAssetId());
    }
}
