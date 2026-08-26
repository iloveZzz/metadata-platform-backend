package com.yss.datamiddle.aicontextlayer.application.service;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.MetadataPlatformGateway;
import com.yss.datamiddle.aicontextlayer.domain.tool.ClassificationQueryResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ClassificationToolApplicationServiceTest {

    private final MetadataPlatformGateway gateway = Mockito.mock(MetadataPlatformGateway.class);
    private final ClassificationToolApplicationService service = new ClassificationToolApplicationService(gateway);

    private static final String BASE_URL = "http://localhost:8080";
    private static final String AGENT_ID = "agent-001";

    @Test
    @DisplayName("正常查询分级分类且内嵌 classificationId 溯源")
    void queryClassificationsSuccess() {
        when(gateway.getClassifications(anyString())).thenReturn("[]");

        ClassificationQueryResult result = service.queryClassifications(AGENT_ID, BASE_URL, 1, 20);
        assertNotNull(result);
        assertEquals(2, result.getTotalCount());
        assertNotNull(result.getItems().get(0).getProvenance());
        assertEquals("cls-001", result.getItems().get(0).getProvenance().getClassificationId());
        assertNull(result.getItems().get(0).getProvenance().getAssetId());
    }

    @Test
    @DisplayName("SEC-07: pageSize 超过 50 抛出 INVALID_PARAMS")
    void pageSizeExceedsLimitThrowsInvalidParams() {
        McpException ex = assertThrows(McpException.class, () ->
                service.queryClassifications(AGENT_ID, BASE_URL, 1, 51)
        );
        assertEquals(McpErrorCode.INVALID_PARAMS, ex.getErrorCode());
    }

    @Test
    @DisplayName("SEC-03: 403 降级返回空列表")
    void upstream403ReturnsEmptyList() {
        when(gateway.getClassifications(anyString())).thenThrow(McpException.of(McpErrorCode.UNAUTHORIZED));

        ClassificationQueryResult result = service.queryClassifications(AGENT_ID, BASE_URL, 1, 20);
        assertNotNull(result);
        assertEquals(0, result.getTotalCount());
        assertTrue(result.getItems().isEmpty());
    }
}
