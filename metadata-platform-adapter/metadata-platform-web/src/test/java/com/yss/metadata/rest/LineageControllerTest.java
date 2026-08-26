package com.yss.metadata.rest;

import com.yss.metadata.application.lineage.service.LineageActionService;
import com.yss.metadata.application.lineage.service.LineageQueryService;
import com.yss.metadata.application.lineage.service.convertor.LineageAppConvertor;
import com.yss.metadata.application.lineage.service.impl.LineageActionServiceImpl;
import com.yss.metadata.application.lineage.service.impl.LineageQueryServiceImpl;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetStatus;
import com.yss.metadata.domain.lineage.model.LineageConfidence;
import com.yss.metadata.domain.lineage.model.LineageEdge;
import com.yss.metadata.domain.lineage.model.LineageType;
import com.yss.metadata.rest.advice.MetadataGlobalExceptionHandler;
import com.yss.metadata.application.asset.support.InMemoryAssetRepository;
import com.yss.metadata.rest.support.InMemoryAuditLogRepository;
import com.yss.metadata.rest.support.InMemoryLineageGraphRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 血缘 REST 契约测试（WU-03-05，冻结 OpenAPI lineage 段）。
 *
 * <p>覆盖：图谱 confidence 筛选/空血缘空结构/图版本 token；人工补录
 * 成功 201 / 成环 CYCLE 409（定位冲突边）/ 版本 token CONFLICT 409 /
 * 重复边幂等 / 404 / 422。</p>
 */
class LineageControllerTest {

    private InMemoryAssetRepository assetRepository;
    private InMemoryLineageGraphRepository graphRepository;
    private InMemoryAuditLogRepository auditLogRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        assetRepository = new InMemoryAssetRepository();
        graphRepository = new InMemoryLineageGraphRepository();
        auditLogRepository = new InMemoryAuditLogRepository();
        LineageQueryService queryService =
                new LineageQueryServiceImpl(assetRepository, graphRepository, org.mapstruct.factory.Mappers.getMapper(LineageAppConvertor.class));
        LineageActionService actionService =
                new LineageActionServiceImpl(graphRepository, assetRepository, auditLogRepository,
                        org.mapstruct.factory.Mappers.getMapper(LineageAppConvertor.class));
        mockMvc = MockMvcBuilders.standaloneSetup(new LineageController(queryService, actionService))
                .setControllerAdvice(new MetadataGlobalExceptionHandler())
                .build();
    }

    // ---------- GET /api/assets/{id}/lineage ----------

    @Test
    @DisplayName("图谱返回 200：Result 包装 + 邻域边 + 图版本 token")
    void graphReturns200() throws Exception {
        seedAsset("a-center", "dwd_order_di");
        graphRepository.seed(edge("e1", "a-up", "a-center", "v3", LineageConfidence.AUTO_HIGH));
        graphRepository.seed(edge("e2", "a-center", "a-down", "v2", LineageConfidence.MANUAL_HIGH));

        mockMvc.perform(get("/api/assets/a-center/lineage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("DM-A0001"))
                .andExpect(jsonPath("$.data.edges", hasSize(2)))
                .andExpect(jsonPath("$.data.edges[0].fromAssetId").value("a-up"))
                .andExpect(jsonPath("$.data.edges[0].confidence").value("auto-high"))
                .andExpect(jsonPath("$.data.edges[1].confidence").value("manual-high"))
                .andExpect(jsonPath("$.data.graphVersionToken").value("v3"));
    }

    @Test
    @DisplayName("confidence 筛选：manual-high 只返回对应边")
    void graphConfidenceFilter() throws Exception {
        seedAsset("a-center", "dwd_order_di");
        graphRepository.seed(edge("e1", "a-center", "a-1", "v2", LineageConfidence.AUTO_HIGH));
        graphRepository.seed(edge("e2", "a-center", "a-2", "v2", LineageConfidence.MANUAL_HIGH));

        mockMvc.perform(get("/api/assets/a-center/lineage").param("confidence", "manual-high"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.edges", hasSize(1)))
                .andExpect(jsonPath("$.data.edges[0].id").value("e2"));
    }

    @Test
    @DisplayName("空血缘返回空结构（200，data.edges 空数组）")
    void emptyGraphReturnsEmptyStructure() throws Exception {
        seedAsset("a-center", "dwd_order_di");

        mockMvc.perform(get("/api/assets/a-center/lineage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.edges", hasSize(0)))
                .andExpect(jsonPath("$.data.graphVersionToken").doesNotExist());
    }

    @Test
    @DisplayName("资产不存在返回 404（asset.not_found）")
    void graphNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/assets/not-exist/lineage"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("asset.not_found"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    @Test
    @DisplayName("非法 confidence 返回 422（asset.param.invalid）")
    void graphInvalidConfidenceReturns422() throws Exception {
        seedAsset("a-center", "dwd_order_di");

        mockMvc.perform(get("/api/assets/a-center/lineage").param("confidence", "unknown"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("asset.param.invalid"));
    }

    // ---------- POST /api/lineage/manual ----------

    @Test
    @DisplayName("补录成功返回 201：边信息 + 审计 lineage.manual")
    void manualReturns201() throws Exception {
        seedAsset("a-from", "ods_order");
        seedAsset("a-to", "dwd_order_di");

        mockMvc.perform(post("/api/lineage/manual")
                        .header("X-User-Id", "u-me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAssetId\":\"a-from\",\"toAssetId\":\"a-to\","
                                + "\"type\":\"manual\",\"confidence\":\"manual-high\","
                                + "\"remark\":\"人工确认\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fromAssetId").value("a-from"))
                .andExpect(jsonPath("$.data.toAssetId").value("a-to"))
                .andExpect(jsonPath("$.data.type").value("manual"))
                .andExpect(jsonPath("$.data.confidence").value("manual-high"))
                .andExpect(jsonPath("$.data.id").isNotEmpty());

        assertThat(auditLogRepository.entries()).hasSize(1);
    }

    @Test
    @DisplayName("成环补录返回 409 CYCLE：定位冲突边（fieldErrors）")
    void manualCycleReturns409() throws Exception {
        seedAsset("a-1", "t1");
        seedAsset("a-2", "t2");
        seedAsset("a-3", "t3");
        graphRepository.seed(edge("e1", "a-1", "a-2", "v0", LineageConfidence.AUTO_HIGH));
        graphRepository.seed(edge("e2", "a-2", "a-3", "v0", LineageConfidence.AUTO_HIGH));

        mockMvc.perform(post("/api/lineage/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAssetId\":\"a-3\",\"toAssetId\":\"a-1\","
                                + "\"type\":\"manual\",\"confidence\":\"manual-high\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("lineage.cycle"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("CYCLE")))
                .andExpect(jsonPath("$.severity").value("error"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("lineage.cycle.edge"))
                .andExpect(jsonPath("$.fieldErrors[0].message")
                        .value(org.hamcrest.Matchers.containsString("a-3→a-1")));
    }

    @Test
    @DisplayName("版本 token 不匹配返回 409 CONFLICT（恢复路径提示）")
    void manualVersionConflictReturns409() throws Exception {
        seedAsset("a-1", "t1");
        seedAsset("a-2", "t2");
        graphRepository.seed(edge("e1", "a-1", "a-2", "v1", LineageConfidence.AUTO_HIGH));

        mockMvc.perform(post("/api/lineage/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAssetId\":\"a-2\",\"toAssetId\":\"a-1\","
                                + "\"type\":\"manual\",\"confidence\":\"manual-high\","
                                + "\"graphVersionToken\":\"stale-token\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("lineage.conflict"))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("graphVersionToken")));
    }

    @Test
    @DisplayName("重复补录幂等：返回既有边 201，不重复写入")
    void manualDuplicateIdempotent() throws Exception {
        seedAsset("a-1", "t1");
        seedAsset("a-2", "t2");
        graphRepository.seed(edge("e1", "a-1", "a-2", "v1", LineageConfidence.MANUAL_HIGH));

        mockMvc.perform(post("/api/lineage/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAssetId\":\"a-1\",\"toAssetId\":\"a-2\","
                                + "\"type\":\"manual\",\"confidence\":\"manual-high\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("e1"));

        assertThat(graphRepository.allEdges()).hasSize(1);
    }

    @Test
    @DisplayName("缺必填字段返回 422（fieldErrors 字段级）")
    void manualMissingFieldReturns422() throws Exception {
        mockMvc.perform(post("/api/lineage/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAssetId\":\"a-1\",\"confidence\":\"manual-high\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("param.invalid"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'toAssetId')]", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'type')]", hasSize(1)));
    }

    @Test
    @DisplayName("非法枚举值返回 422（请求体解析失败）")
    void manualInvalidEnumReturns422() throws Exception {
        mockMvc.perform(post("/api/lineage/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAssetId\":\"a-1\",\"toAssetId\":\"a-2\","
                                + "\"type\":\"manual\",\"confidence\":\"bogus\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("param.invalid"));
    }

    @Test
    @DisplayName("补录资产不存在返回 404")
    void manualAssetNotFoundReturns404() throws Exception {
        seedAsset("a-1", "t1");

        mockMvc.perform(post("/api/lineage/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAssetId\":\"not-exist\",\"toAssetId\":\"a-1\","
                                + "\"type\":\"manual\",\"confidence\":\"manual-high\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("asset.not_found"));
    }

    // ---------- 辅助 ----------

    private void seedAsset(String id, String name) {
        assetRepository.seedSourceName("s-1", "交易中心主库");
        assetRepository.seed(Asset.builder().id(id).sourceId("s-1").name(name).type("table")
                .domain("交易域").owner(null).classification("内部").status(AssetStatus.CLAIMED)
                .updatedAt(LocalDateTime.of(2026, 8, 10, 9, 12)).build());
    }

    private LineageEdge edge(String id, String from, String to, String version,
                             LineageConfidence confidence) {
        return LineageEdge.builder().id(id).fromAssetId(from).toAssetId(to)
                .type(LineageType.SQL).confidence(confidence).graphVersion(version).build();
    }
}
