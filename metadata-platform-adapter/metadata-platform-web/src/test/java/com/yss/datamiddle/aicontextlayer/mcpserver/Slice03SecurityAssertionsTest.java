package com.yss.datamiddle.aicontextlayer.mcpserver;

import com.yss.datamiddle.aicontextlayer.application.service.AgentDomainMappingService;
import com.yss.datamiddle.aicontextlayer.application.service.LineageToolApplicationService;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ReadOnlyToolRegistry;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.MetadataPlatformGateway;
import com.yss.datamiddle.aicontextlayer.domain.tool.ImpactAnalysisResult;
import com.yss.datamiddle.aicontextlayer.domain.tool.LineageGraphResult;
import com.yss.datamiddle.aicontextlayer.mcpserver.interceptor.MethodInterceptor;
import com.yss.datamiddle.aicontextlayer.mcpserver.tool.ImpactAnalysisToolHandler;
import com.yss.datamiddle.aicontextlayer.mcpserver.tool.LineageToolHandler;
import com.yss.datamiddle.aicontextlayer.mcpserver.tool.McpToolDispatcher;
import com.yss.datamiddle.aicontextlayer.mcpserver.tool.McpToolHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class Slice03SecurityAssertionsTest {

    private MetadataPlatformGateway metadataGateway;
    private AgentDomainMappingService domainService;
    private LineageToolApplicationService lineageService;
    private McpToolDispatcher dispatcher;

    private static final String BASE_URL = "http://localhost:8080";
    private static final String AGENT_ID = "agent-001";

    @BeforeEach
    void setUp() {
        metadataGateway = Mockito.mock(MetadataPlatformGateway.class);
        domainService = new AgentDomainMappingService(null);
        lineageService = new LineageToolApplicationService(metadataGateway, domainService);

        ReadOnlyToolRegistry registry = new ReadOnlyToolRegistry();
        MethodInterceptor interceptor = new MethodInterceptor(registry);

        List<McpToolHandler> handlers = Arrays.asList(
                new LineageToolHandler(lineageService),
                new ImpactAnalysisToolHandler(lineageService)
        );
        dispatcher = new McpToolDispatcher(interceptor, handlers);
    }

    @Test
    @DisplayName("断言 1: 跨域血缘图悬空边过滤与溯源内嵌")
    void lineageGraphDanglingEdgesDropped() {
        when(metadataGateway.getLineage(anyString(), anyString(), anyInt())).thenReturn("{}");

        Map<String, Object> args = new HashMap<>();
        args.put("asset_id", "ast-pub-root");

        LineageGraphResult result = (LineageGraphResult) dispatcher.dispatch(AGENT_ID, BASE_URL, "lineage", args);
        assertNotNull(result);
        assertEquals(2, result.getNodes().size());
        assertEquals(1, result.getEdges().size());
        assertNotNull(result.getNodes().get(0).getProvenance());
        assertNotNull(result.getEdges().get(0).getProvenance());
    }

    @Test
    @DisplayName("断言 1: 影响分析全量召回经二次校验后重新按深度分组")
    void impactAnalysisFilteredAndRegrouped() {
        when(metadataGateway.getImpactAnalysis(anyString(), anyString(), anyInt())).thenReturn("{}");

        Map<String, Object> args = new HashMap<>();
        args.put("asset_id", "ast-pub-root");

        ImpactAnalysisResult result = (ImpactAnalysisResult) dispatcher.dispatch(AGENT_ID, BASE_URL, "impact_analysis", args);
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getDepthGroups().size());
        assertNotNull(result.getDepthGroups().get(0).getItems().get(0).getProvenance());
    }

    @Test
    @DisplayName("断言 3: 起点资产不存在或 403 统一映射为 ASSET_NOT_FOUND")
    void startAssetUnauthorizedUnifiedHide() {
        when(metadataGateway.getLineage(anyString(), anyString(), anyInt()))
                .thenThrow(McpException.of(McpErrorCode.UNAUTHORIZED));

        Map<String, Object> args = new HashMap<>();
        args.put("asset_id", "ast-forbidden");

        McpException ex = assertThrows(McpException.class, () ->
                dispatcher.dispatch(AGENT_ID, BASE_URL, "lineage", args)
        );
        assertEquals(McpErrorCode.ASSET_NOT_FOUND, ex.getErrorCode());
    }
}
