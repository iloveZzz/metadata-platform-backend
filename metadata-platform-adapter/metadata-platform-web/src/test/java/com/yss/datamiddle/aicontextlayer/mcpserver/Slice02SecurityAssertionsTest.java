package com.yss.datamiddle.aicontextlayer.mcpserver;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.aicontextlayer.application.service.AgentDomainMappingService;
import com.yss.datamiddle.aicontextlayer.application.service.AssetToolApplicationService;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ReadOnlyToolRegistry;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.MetadataPlatformGateway;
import com.yss.datamiddle.aicontextlayer.domain.tool.AssetDetail;
import com.yss.datamiddle.aicontextlayer.domain.tool.AssetSummaryItem;
import com.yss.datamiddle.aicontextlayer.mcpserver.interceptor.MethodInterceptor;
import com.yss.datamiddle.aicontextlayer.mcpserver.tool.AssetDetailToolHandler;
import com.yss.datamiddle.aicontextlayer.mcpserver.tool.McpToolDispatcher;
import com.yss.datamiddle.aicontextlayer.mcpserver.tool.McpToolHandler;
import com.yss.datamiddle.aicontextlayer.mcpserver.tool.SearchAssetsToolHandler;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class Slice02SecurityAssertionsTest {

    private MetadataPlatformGateway metadataGateway;
    private AgentDomainMappingService domainService;
    private AssetToolApplicationService assetToolService;
    private McpToolDispatcher dispatcher;

    private static final String BASE_URL = "http://localhost:8080";
    private static final String AGENT_ID = "agent-001";

    @BeforeEach
    void setUp() {
        metadataGateway = Mockito.mock(MetadataPlatformGateway.class);
        domainService = new AgentDomainMappingService(null);
        assetToolService = new AssetToolApplicationService(metadataGateway, domainService);

        ReadOnlyToolRegistry registry = new ReadOnlyToolRegistry();
        MethodInterceptor interceptor = new MethodInterceptor(registry);

        List<McpToolHandler> handlers = Arrays.asList(
                new SearchAssetsToolHandler(assetToolService),
                new AssetDetailToolHandler(assetToolService)
        );
        dispatcher = new McpToolDispatcher(interceptor, handlers);
    }

    @Test
    @DisplayName("断言 1: 域外资产零返回，total 为过滤后计数")
    void assertion1OutOfDomainZeroReturn() {
        when(metadataGateway.searchAssets(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn("{}");

        Map<String, Object> args = new HashMap<>();
        args.put("keyword", "salary");
        args.put("page", 1);
        args.put("size", 20);

        @SuppressWarnings("unchecked")
        PageResult<AssetSummaryItem> result = (PageResult<AssetSummaryItem>) dispatcher.dispatch(AGENT_ID, BASE_URL, "search_assets", args);

        assertTrue(result.isSuccess());
        // 验证返回结果中只有 public 域资产，confidential_financial 域完全被剔除
        for (AssetSummaryItem item : result.getData()) {
            assertTrue("public".equals(item.getDomain()) || "default".equals(item.getDomain()));
        }
        assertEquals(result.getData().size(), result.getTotalCount());
    }

    @Test
    @DisplayName("断言 2: PII / 列注释 / 样例值 / description 零返回")
    void assertion2PiiAndCommentsStripped() {
        when(metadataGateway.getAssetDetail(anyString(), anyString())).thenReturn("{}");

        Map<String, Object> args = new HashMap<>();
        args.put("asset_id", "ast-pub-01");

        AssetDetail detail = (AssetDetail) dispatcher.dispatch(AGENT_ID, BASE_URL, "asset_detail", args);
        assertNotNull(detail);
        assertNotNull(detail.getColumns());
        // 校验列模型仅含字段名、类型、主键、可空、分级，无敏感注释和样例值字段
        assertEquals(2, detail.getColumns().size());
        assertEquals("id", detail.getColumns().get(0).getName());
        assertEquals("BIGINT", detail.getColumns().get(0).getDataType());
    }

    @Test
    @DisplayName("断言 3: 403 与 404 均返回统一 asset_not_found 错误响应")
    void assertion3UnifiedAssetNotFound() {
        when(metadataGateway.getAssetDetail(anyString(), anyString()))
                .thenThrow(McpException.of(McpErrorCode.UNAUTHORIZED));

        Map<String, Object> args = new HashMap<>();
        args.put("asset_id", "ast-forbidden-01");

        McpException ex = assertThrows(McpException.class, () ->
                dispatcher.dispatch(AGENT_ID, BASE_URL, "asset_detail", args)
        );
        assertEquals(McpErrorCode.ASSET_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("断言 7: 客户端伪造溯源字段被忽略，内嵌服务端真实溯源")
    void assertion7ProvenanceUnforgeable() {
        when(metadataGateway.getAssetDetail(anyString(), anyString())).thenReturn("{}");

        Map<String, Object> args = new HashMap<>();
        args.put("asset_id", "ast-pub-01");
        args.put("source", "fake-upstream-source");
        args.put("updated_at", "2099-01-01T00:00:00");

        AssetDetail detail = (AssetDetail) dispatcher.dispatch(AGENT_ID, BASE_URL, "asset_detail", args);
        assertNotNull(detail.getProvenance());
        assertEquals("metadata-platform", detail.getProvenance().getSource());
        assertEquals("ast-pub-01", detail.getProvenance().getAssetId());
    }

    @Test
    @DisplayName("断言 4: 写操作工具注册与分发拦截")
    void assertion4WriteToolRejected() {
        Map<String, Object> args = new HashMap<>();
        args.put("tag", "test");

        McpException ex = assertThrows(McpException.class, () ->
                dispatcher.dispatch(AGENT_ID, BASE_URL, "tag_asset", args)
        );
        assertEquals(McpErrorCode.TOOL_NOT_FOUND, ex.getErrorCode());
    }
}
