package com.yss.metadata.rest;

import com.yss.metadata.application.asset.support.InMemoryAssetRepository;
import com.yss.metadata.application.lineage.service.ColumnLineageAppService;
import com.yss.metadata.application.lineage.service.convertor.LineageAppConvertor;
import com.yss.metadata.application.lineage.service.impl.ColumnLineageAppServiceImpl;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetColumn;
import com.yss.metadata.domain.asset.model.AssetStatus;
import com.yss.metadata.domain.lineage.model.LineageConfidence;
import com.yss.metadata.domain.lineage.model.LineageEdge;
import com.yss.metadata.domain.lineage.model.LineageType;
import com.yss.metadata.rest.advice.MetadataGlobalExceptionHandler;
import com.yss.metadata.rest.support.InMemoryAuditLogRepository;
import com.yss.metadata.rest.support.InMemoryLineageGraphRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 字段级血缘 REST 契约测试。
 */
class ColumnLineageControllerTest {

    private InMemoryAssetRepository assetRepository;
    private InMemoryLineageGraphRepository graphRepository;
    private InMemoryAuditLogRepository auditLogRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        assetRepository = new InMemoryAssetRepository();
        graphRepository = new InMemoryLineageGraphRepository();
        auditLogRepository = new InMemoryAuditLogRepository();

        ColumnLineageAppService columnService = new ColumnLineageAppServiceImpl(
                graphRepository,
                assetRepository,
                auditLogRepository,
                Mappers.getMapper(LineageAppConvertor.class)
        );

        mockMvc = MockMvcBuilders.standaloneSetup(new ColumnLineageController(
                columnService,
                new com.yss.metadata.infrastructure.lineage.parser.JSqlParserLineageParserImpl()
        ))
                .setControllerAdvice(new MetadataGlobalExceptionHandler())
                .build();

        seedAssets();
    }

    private void seedAssets() {
        assetRepository.seedSourceName("s-1", "测试库");

        Asset src = Asset.builder()
                .id("ast-orders")
                .sourceId("s-1")
                .name("ods_orders")
                .status(AssetStatus.CLAIMED)
                .build();
        assetRepository.seed(src);
        assetRepository.seedColumns("ast-orders", Arrays.asList(
                AssetColumn.builder().id("col-order-id").name("order_id").type("BIGINT").pk(true).build(),
                AssetColumn.builder().id("col-amount").name("amount").type("DECIMAL").classification("S2").build()
        ));

        Asset tgt = Asset.builder()
                .id("ast-dwd")
                .sourceId("s-1")
                .name("dwd_orders")
                .status(AssetStatus.CLAIMED)
                .build();
        assetRepository.seed(tgt);
        assetRepository.seedColumns("ast-dwd", Arrays.asList(
                AssetColumn.builder().id("col-tgt-id").name("order_id").type("BIGINT").pk(true).build(),
                AssetColumn.builder().id("col-tgt-amt").name("order_amt").type("DECIMAL").classification("S2").build()
        ));
    }

    @Test
    @DisplayName("GET 字段血缘图谱：返回包含字段节点与血缘边")
    void testGetColumnLineageGraph() throws Exception {
        graphRepository.seed(LineageEdge.builder()
                .id("edge-1")
                .fromAssetId("ast-orders")
                .fromColumnId("order_id")
                .toAssetId("ast-dwd")
                .toColumnId("order_id")
                .transformExpr("order_id")
                .exprType("DIRECT")
                .type(LineageType.SQL)
                .confidence(LineageConfidence.AUTO_HIGH)
                .graphVersion("v1")
                .build());

        mockMvc.perform(get("/api/assets/ast-orders/column-lineage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.centerAssetId", is("ast-orders")))
                .andExpect(jsonPath("$.data.nodes", hasSize(4)))
                .andExpect(jsonPath("$.data.edges", hasSize(1)))
                .andExpect(jsonPath("$.data.edges[0].fromColumnId", is("order_id")))
                .andExpect(jsonPath("$.data.edges[0].toColumnId", is("order_id")))
                .andExpect(jsonPath("$.data.edges[0].exprType", is("DIRECT")));
    }

    @Test
    @DisplayName("POST 人工补录字段血缘：成功 201")
    void testAddManualColumnEdgeSuccess() throws Exception {
        String json = "{\n" +
                "  \"fromAssetId\": \"ast-orders\",\n" +
                "  \"fromColumnId\": \"amount\",\n" +
                "  \"toAssetId\": \"ast-dwd\",\n" +
                "  \"toColumnId\": \"order_amt\",\n" +
                "  \"transformExpr\": \"amount * 0.9\",\n" +
                "  \"exprType\": \"COMPUTED\",\n" +
                "  \"remark\": \"手工补录折扣计算\"\n" +
                "}";

        mockMvc.perform(post("/api/lineage/column/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fromAssetId", is("ast-orders")))
                .andExpect(jsonPath("$.data.fromColumnId", is("amount")))
                .andExpect(jsonPath("$.data.toAssetId", is("ast-dwd")))
                .andExpect(jsonPath("$.data.toColumnId", is("order_amt")))
                .andExpect(jsonPath("$.data.exprType", is("COMPUTED")));
    }

    @Test
    @DisplayName("POST 人工补录字段血缘：成环阻断 409 CYCLE")
    void testAddManualColumnEdgeCycle() throws Exception {
        graphRepository.seed(LineageEdge.builder()
                .id("edge-1")
                .fromAssetId("ast-orders")
                .fromColumnId("amount")
                .toAssetId("ast-dwd")
                .toColumnId("order_amt")
                .type(LineageType.SQL)
                .confidence(LineageConfidence.AUTO_HIGH)
                .graphVersion("v1")
                .build());

        // 尝试反向补录 dwd.order_amt -> orders.amount 构成闭环
        String cycleJson = "{\n" +
                "  \"fromAssetId\": \"ast-dwd\",\n" +
                "  \"fromColumnId\": \"order_amt\",\n" +
                "  \"toAssetId\": \"ast-orders\",\n" +
                "  \"toColumnId\": \"amount\"\n" +
                "}";

        mockMvc.perform(post("/api/lineage/column/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cycleJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("lineage.cycle")));
    }

    @Test
    @DisplayName("DELETE 字段血缘边：成功 200")
    void testDeleteColumnEdge() throws Exception {
        graphRepository.seed(LineageEdge.builder()
                .id("edge-to-del")
                .fromAssetId("ast-orders")
                .fromColumnId("order_id")
                .toAssetId("ast-dwd")
                .toColumnId("order_id")
                .build());

        mockMvc.perform(delete("/api/lineage/column/edge-to-del"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/lineage/sql/parse 实时 SQL AST 解析成功")
    void testParseSqlLineage() throws Exception {
        String sqlJson = "{\n" +
                "  \"sql\": \"CREATE VIEW dwd_orders AS SELECT order_id, amount * 0.9 AS discounted_amt FROM ods_orders\"\n" +
                "}";

        mockMvc.perform(post("/api/lineage/sql/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sqlJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supported", is(true)))
                .andExpect(jsonPath("$.data.columnLineage", hasSize(2)));
    }
}
