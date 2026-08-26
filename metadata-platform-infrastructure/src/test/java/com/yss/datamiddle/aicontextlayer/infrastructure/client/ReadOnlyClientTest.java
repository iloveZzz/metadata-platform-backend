package com.yss.datamiddle.aicontextlayer.infrastructure.client;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadOnlyClientTest {

    private final BaseUrlWhitelistValidator validator = new BaseUrlWhitelistValidator();
    private final A2GatewayStub a2GatewayStub = new A2GatewayStub();
    private final ReadOnlyClient client = new ReadOnlyClient(validator, a2GatewayStub);

    private static final String VALID_BASE_URL = "http://localhost:8080";

    @Test
    @DisplayName("5 个只读端点在合法 Base URL 下正常调用")
    void fiveReadOnlyEndpointsSucceed() {
        assertNotNull(client.searchAssets(VALID_BASE_URL, "order", 1, 10));
        assertNotNull(client.getAssetDetail(VALID_BASE_URL, "asset-001"));
        assertNotNull(client.getLineage(VALID_BASE_URL, "asset-001", 3));
        assertNotNull(client.getImpactAnalysis(VALID_BASE_URL, "asset-001", 2));
        assertNotNull(client.getClassifications(VALID_BASE_URL));
    }

    @Test
    @DisplayName("非白名单 URL 触发 SSRF 拦截")
    void nonWhitelistUrlBlocked() {
        McpException ex = assertThrows(McpException.class, () ->
            client.searchAssets("http://evil-server.com", "test", 1, 10)
        );
        assertEquals(McpErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    @DisplayName("空 assetId 抛出 INVALID_PARAMS")
    void emptyAssetIdThrowsInvalidParams() {
        McpException ex = assertThrows(McpException.class, () ->
            client.getAssetDetail(VALID_BASE_URL, "")
        );
        assertEquals(McpErrorCode.INVALID_PARAMS, ex.getErrorCode());
    }

    @Test
    @DisplayName("A2GatewayStub 预留接口可调用且返回空列表")
    void a2StubReturnsEmptyList() {
        assertTrue(client.getA2GatewayStub().queryMetricDefinitions("revenue").isEmpty());
    }
}
