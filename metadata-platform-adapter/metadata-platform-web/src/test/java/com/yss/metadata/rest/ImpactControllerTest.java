package com.yss.metadata.rest;

import com.yss.metadata.application.lineage.service.ImpactAnalysisService;
import com.yss.metadata.application.lineage.service.convertor.LineageAppConvertor;
import com.yss.metadata.application.lineage.service.impl.ImpactAnalysisServiceImpl;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetStatus;
import com.yss.metadata.domain.lineage.model.ExportTask;
import com.yss.metadata.domain.lineage.model.ExportTaskStatus;
import com.yss.metadata.domain.lineage.model.LineageConfidence;
import com.yss.metadata.domain.lineage.model.LineageEdge;
import com.yss.metadata.domain.lineage.model.LineageType;
import com.yss.metadata.rest.advice.MetadataGlobalExceptionHandler;
import com.yss.metadata.rest.support.FakeExportFileStorage;
import com.yss.metadata.application.asset.support.InMemoryAssetRepository;
import com.yss.metadata.rest.support.InMemoryAuditLogRepository;
import com.yss.metadata.rest.support.InMemoryExportTaskRepository;
import com.yss.metadata.rest.support.InMemoryImpactAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 影响分析 REST 契约测试（WU-03-05，冻结 OpenAPI impact 段）。
 *
 * <p>覆盖：影响分析深度分组/sortBy/0 影响空结构/404/422；导出 202 ExportTask/
 * 幂等复用/CSV-JSON 生成/审计/404/422。</p>
 */
class ImpactControllerTest {

    private InMemoryAssetRepository assetRepository;
    private InMemoryImpactAnalysisRepository impactRepository;
    private InMemoryExportTaskRepository exportTaskRepository;
    private InMemoryAuditLogRepository auditLogRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        assetRepository = new InMemoryAssetRepository();
        impactRepository = new InMemoryImpactAnalysisRepository();
        exportTaskRepository = new InMemoryExportTaskRepository();
        auditLogRepository = new InMemoryAuditLogRepository();
        ImpactAnalysisService service = new ImpactAnalysisServiceImpl(
                impactRepository, exportTaskRepository, new FakeExportFileStorage(),
                auditLogRepository, assetRepository, org.mapstruct.factory.Mappers.getMapper(LineageAppConvertor.class));
        mockMvc = MockMvcBuilders.standaloneSetup(new ImpactController(service))
                .setControllerAdvice(new MetadataGlobalExceptionHandler())
                .build();
    }

    // ---------- GET /api/assets/{id}/impact-analysis ----------

    @Test
    @DisplayName("影响分析返回 200：深度分组（直接/间接）+ 资产组合字段")
    void impactReturns200WithGroups() throws Exception {
        seedAsset("a-root", "dwd_order_di");
        seedDownstream("a-root", "a-1", "ads_order_1d", "交易域", "内部");
        seedDownstream("a-1", "a-2", "bi_report_2d", "财务域", "敏感-PII");

        mockMvc.perform(get("/api/assets/a-root/impact-analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sortBy").value("depth"))
                .andExpect(jsonPath("$.data.groups", hasSize(2)))
                .andExpect(jsonPath("$.data.groups[0].depth").value(1))
                .andExpect(jsonPath("$.data.groups[0].items[0].name").value("ads_order_1d"))
                .andExpect(jsonPath("$.data.groups[0].items[0].risk").value("medium"))
                .andExpect(jsonPath("$.data.groups[1].depth").value(2))
                .andExpect(jsonPath("$.data.groups[1].items[0].assetId").value("a-2"));
    }

    @Test
    @DisplayName("sortBy=risk：组内风险降序（敏感 high 优先）")
    void impactSortByRisk() throws Exception {
        seedAsset("a-root", "dwd_order_di");
        seedDownstream("a-root", "a-low", "low_table", "交易域", null);
        seedDownstream("a-root", "a-high", "high_table", "交易域", "敏感-PII");

        mockMvc.perform(get("/api/assets/a-root/impact-analysis").param("sortBy", "risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groups[0].items[0].assetId").value("a-high"))
                .andExpect(jsonPath("$.data.groups[0].items[0].risk").value("high"))
                .andExpect(jsonPath("$.data.groups[0].items[1].assetId").value("a-low"))
                .andExpect(jsonPath("$.data.groups[0].items[1].risk").value("low"));
    }

    @Test
    @DisplayName("0 影响返回空结构（200，groups 空数组，非错误）")
    void emptyImpactReturns200Empty() throws Exception {
        seedAsset("a-root", "dwd_order_di");

        mockMvc.perform(get("/api/assets/a-root/impact-analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groups", hasSize(0)));
    }

    @Test
    @DisplayName("资产不存在返回 404；非法 sortBy 返回 422")
    void impactValidation() throws Exception {
        mockMvc.perform(get("/api/assets/not-exist/impact-analysis"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("asset.not_found"));

        seedAsset("a-root", "dwd_order_di");
        mockMvc.perform(get("/api/assets/a-root/impact-analysis").param("sortBy", "unknown"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("asset.param.invalid"));
    }

    // ---------- GET /api/assets/{id}/impact-analysis/export ----------

    @Test
    @DisplayName("导出返回 202：ExportTask 信息（success + fileRef + operator 审计上下文）")
    void exportReturns202() throws Exception {
        seedAsset("a-root", "dwd_order_di");
        seedDownstream("a-root", "a-1", "ads_order_1d", "交易域", "内部");

        mockMvc.perform(get("/api/assets/a-root/impact-analysis/export")
                        .header("X-User-Id", "u-me"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.assetId").value("a-root"))
                .andExpect(jsonPath("$.data.format").value("csv"))
                .andExpect(jsonPath("$.data.status").value("success"))
                .andExpect(jsonPath("$.data.operator").value("u-me"))
                .andExpect(jsonPath("$.data.fileRef").isNotEmpty());

        assertThat(auditLogRepository.entries()).hasSize(1);
    }

    @Test
    @DisplayName("导出 JSON：202 + JSON 文件生成")
    void exportJsonReturns202() throws Exception {
        seedAsset("a-root", "dwd_order_di");
        seedDownstream("a-root", "a-1", "ads_order_1d", "交易域", "内部");

        mockMvc.perform(get("/api/assets/a-root/impact-analysis/export").param("format", "json"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("success"))
                .andExpect(jsonPath("$.data.format").value("json"));
    }

    @Test
    @DisplayName("导出幂等：同资产同格式进行中任务复用（202 返回既有任务）")
    void exportIdempotentReuse() throws Exception {
        seedAsset("a-root", "dwd_order_di");
        exportTaskRepository.seed(ExportTask.builder().id("task-1").assetId("a-root").format("csv")
                .status(ExportTaskStatus.RUNNING).operator("u-other")
                .createdAt(LocalDateTime.now()).build());

        mockMvc.perform(get("/api/assets/a-root/impact-analysis/export")
                        .header("X-User-Id", "u-me"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.id").value("task-1"))
                .andExpect(jsonPath("$.data.status").value("running"))
                .andExpect(jsonPath("$.data.operator").value("u-other"));

        assertThat(exportTaskRepository.all()).hasSize(1);
    }

    @Test
    @DisplayName("导出校验：资产不存在 404；非法格式 422")
    void exportValidation() throws Exception {
        mockMvc.perform(get("/api/assets/not-exist/impact-analysis/export"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("asset.not_found"));

        seedAsset("a-root", "dwd_order_di");
        mockMvc.perform(get("/api/assets/a-root/impact-analysis/export").param("format", "xlsx"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("asset.param.invalid"));
    }

    // ---------- 辅助 ----------

    private void seedAsset(String id, String name) {
        assetRepository.seedSourceName("s-1", "交易中心主库");
        assetRepository.seed(Asset.builder().id(id).sourceId("s-1").name(name).type("table")
                .domain("交易域").owner(null).classification("内部").status(AssetStatus.CLAIMED)
                .updatedAt(LocalDateTime.of(2026, 8, 10, 9, 12)).build());
    }

    private void seedDownstream(String from, String to, String name, String domain, String classification) {
        impactRepository.seedNode(to, name, "table", domain, classification);
        impactRepository.seedEdge(LineageEdge.builder().id("e-" + to).fromAssetId(from).toAssetId(to)
                .type(LineageType.SQL).confidence(LineageConfidence.AUTO_HIGH).build());
    }
}
