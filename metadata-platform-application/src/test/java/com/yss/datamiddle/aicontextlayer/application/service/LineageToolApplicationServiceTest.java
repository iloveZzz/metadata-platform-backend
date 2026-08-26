package com.yss.datamiddle.aicontextlayer.application.service;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.MetadataPlatformGateway;
import com.yss.datamiddle.aicontextlayer.domain.tool.ImpactAnalysisResult;
import com.yss.datamiddle.aicontextlayer.domain.tool.LineageGraphResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class LineageToolApplicationServiceTest {

    private final MetadataPlatformGateway metadataGateway = Mockito.mock(MetadataPlatformGateway.class);
    private final AgentDomainMappingService domainService = new AgentDomainMappingService(null);
    private final LineageToolApplicationService service = new LineageToolApplicationService(metadataGateway, domainService);

    private static final String BASE_URL = "http://localhost:8080";
    private static final String AGENT_ID = "agent-001";

    @Test
    @DisplayName("断言 1: 血缘图中域外节点剔除且悬空边不返回")
    void lineageOutOfDomainNodesAndDanglingEdgesDropped() {
        when(metadataGateway.getLineage(anyString(), anyString(), anyInt())).thenReturn("{}");

        LineageGraphResult result = service.getLineage(AGENT_ID, BASE_URL, "ast-pub-root", "all");
        assertNotNull(result);
        // 验证 3 个节点中 confidential_financial 域节点被剔除，仅剩 2 个 public 节点
        assertEquals(2, result.getNodes().size());
        // 验证 2 条边中指向 confidential_financial 域的边被作为悬空边剔除，仅剩 1 条边
        assertEquals(1, result.getEdges().size());
        assertEquals("ast-pub-root", result.getEdges().get(0).getFromId());
        assertEquals("ast-dwd-01", result.getEdges().get(0).getToId());
    }

    @Test
    @DisplayName("断言 1: 影响分析全量召回经域过滤后重新按深度分组")
    void impactAnalysisFilteredAndRegrouped() {
        when(metadataGateway.getImpactAnalysis(anyString(), anyString(), anyInt())).thenReturn("{}");

        ImpactAnalysisResult result = service.getImpactAnalysis(AGENT_ID, BASE_URL, "ast-pub-root", "depth");
        assertNotNull(result);
        // 2 个影响项中 1 个域外被过滤，仅剩 1 个
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getDepthGroups().size());
        assertEquals(1, result.getDepthGroups().get(0).getDepth());
        assertEquals("ast-dwd-01", result.getDepthGroups().get(0).getItems().get(0).getAssetId());
    }

    @Test
    @DisplayName("断言 3: 起点资产 403 统一映射为 ASSET_NOT_FOUND")
    void startAsset403MappedToAssetNotFound() {
        when(metadataGateway.getLineage(anyString(), anyString(), anyInt()))
                .thenThrow(McpException.of(McpErrorCode.UNAUTHORIZED));

        McpException ex = assertThrows(McpException.class, () ->
                service.getLineage(AGENT_ID, BASE_URL, "ast-forbidden", "all")
        );
        assertEquals(McpErrorCode.ASSET_NOT_FOUND, ex.getErrorCode());
    }
}
