package com.yss.metadata.application.lineage.service;

import com.yss.metadata.application.asset.support.InMemoryAssetRepository;
import com.yss.metadata.application.lineage.service.impl.ImpactAnalysisServiceImpl;
import com.yss.metadata.application.lineage.support.FakeExportFileStorage;
import com.yss.metadata.application.lineage.support.InMemoryAuditLogRepository;
import com.yss.metadata.application.lineage.support.InMemoryExportTaskRepository;
import com.yss.metadata.application.lineage.support.InMemoryImpactAnalysisRepository;
import com.yss.metadata.client.vo.ExportTaskVO;
import com.yss.metadata.client.vo.ImpactGroupVO;
import com.yss.metadata.client.vo.ImpactItemVO;
import com.yss.metadata.client.vo.ImpactVO;
import com.yss.metadata.domain.asset.exception.AssetNotFoundException;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetStatus;
import com.yss.metadata.domain.audit.model.AuditLogEntry;
import com.yss.metadata.domain.lineage.model.ExportTask;
import com.yss.metadata.domain.lineage.model.ExportTaskStatus;
import com.yss.metadata.domain.lineage.model.LineageConfidence;
import com.yss.metadata.domain.lineage.model.LineageEdge;
import com.yss.metadata.domain.lineage.model.LineageType;
import com.yss.metadata.application.lineage.service.convertor.LineageAppConvertor;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 影响分析应用服务测试（WU-03-03 召回分组 / WU-03-04 导出任务）。
 *
 * <p>影响分析：全量召回/深度分组/sortBy depth-domain-risk/环保护/深度上限/0 影响空结构。
 * 导出：202 幂等复用/CSV-JSON 生成/状态流转/审计/失败标记。</p>
 */
class ImpactAnalysisServiceTest {

    private InMemoryAssetRepository assetRepository;
    private InMemoryImpactAnalysisRepository impactRepository;
    private InMemoryExportTaskRepository exportTaskRepository;
    private FakeExportFileStorage exportFileStorage;
    private InMemoryAuditLogRepository auditLogRepository;
    private ImpactAnalysisService service;

    @BeforeEach
    void setUp() {
        assetRepository = new InMemoryAssetRepository();
        impactRepository = new InMemoryImpactAnalysisRepository();
        exportTaskRepository = new InMemoryExportTaskRepository();
        exportFileStorage = new FakeExportFileStorage();
        auditLogRepository = new InMemoryAuditLogRepository();
        service = new ImpactAnalysisServiceImpl(impactRepository, exportTaskRepository,
                exportFileStorage, auditLogRepository, assetRepository, org.mapstruct.factory.Mappers.getMapper(LineageAppConvertor.class));
    }

    // ---------- 影响分析 ----------

    @Test
    @DisplayName("下游全量召回并按深度分组（1=直接，2=间接）")
    void impactGroupsByDepth() {
        seedAsset("a-root", "dwd_order_di");
        seedDownstream("a-root", "a-1", "ads_order_1d", "交易域", "内部");
        seedDownstream("a-1", "a-2", "bi_report_2d", "财务域", "敏感-PII");
        seedDownstream("a-root", "a-3", "dim_date", "公共域", null);

        ImpactVO vo = service.getImpact("a-root", "depth");

        assertThat(vo.getSortBy()).isEqualTo("depth");
        List<ImpactGroupVO> groups = vo.getGroups();
        assertThat(groups).hasSize(2);
        assertThat(groups.get(0).getDepth()).isEqualTo(1);
        assertThat(groups.get(0).getItems()).extracting(ImpactItemVO::getAssetId)
                .containsExactlyInAnyOrder("a-1", "a-3");
        assertThat(groups.get(1).getDepth()).isEqualTo(2);
        assertThat(groups.get(1).getItems()).extracting(ImpactItemVO::getAssetId)
                .containsExactly("a-2");
    }

    @Test
    @DisplayName("sortBy=domain：组内按数据域排序（空域后置）")
    void impactSortByDomain() {
        seedAsset("a-root", "dwd_order_di");
        seedDownstream("a-root", "a-b", "b_table", "交易域", "内部");
        seedDownstream("a-root", "a-a", "a_table", null, "内部");
        seedDownstream("a-root", "a-c", "c_table", "财务域", "内部");

        ImpactVO vo = service.getImpact("a-root", "domain");

        // Java String.compareTo 为 UTF-16 码点序：交(0x4EA4) < 财(0x8D22)，空域后置
        assertThat(vo.getGroups().get(0).getItems()).extracting(ImpactItemVO::getAssetId)
                .containsExactly("a-b", "a-c", "a-a");
    }

    @Test
    @DisplayName("sortBy=risk：组内按风险降序（敏感→内部），含风险推导")
    void impactSortByRisk() {
        seedAsset("a-root", "dwd_order_di");
        seedDownstream("a-root", "a-low", "low_table", "交易域", null);
        seedDownstream("a-root", "a-mid", "mid_table", "交易域", "内部");
        seedDownstream("a-root", "a-high", "high_table", "交易域", "敏感-PII");

        ImpactVO vo = service.getImpact("a-root", "risk");

        List<ImpactItemVO> items = vo.getGroups().get(0).getItems();
        assertThat(items).extracting(ImpactItemVO::getAssetId)
                .containsExactly("a-high", "a-mid", "a-low");
        assertThat(items).extracting(ImpactItemVO::getRisk)
                .containsExactly("high", "medium", "low");
    }

    @Test
    @DisplayName("0 影响返回空分组（非错误）")
    void emptyImpactReturnsEmptyGroups() {
        seedAsset("a-root", "dwd_order_di");

        ImpactVO vo = service.getImpact("a-root", "depth");

        assertThat(vo.getGroups()).isEmpty();
    }

    @Test
    @DisplayName("环保护 + 深度上限：成环图不无限递归，深度不超过上限")
    void cycleProtectionAndDepthCap() {
        seedAsset("a-root", "dwd_order_di");
        // 环：a-1 → a-2 → a-1
        seedEdgeRaw("e-c1", "a-1", "a-2");
        seedEdgeRaw("e-c2", "a-2", "a-1");
        // 链 12 跳（超过深度上限 10）
        seedChain("a-root", 12);

        ImpactVO vo = service.getImpact("a-root", "depth");

        assertThat(vo.getGroups()).isNotEmpty();
        assertThat(vo.getGroups()).allSatisfy(group ->
                assertThat(group.getDepth()).isLessThanOrEqualTo(ImpactAnalysisServiceImpl.MAX_IMPACT_DEPTH));
    }

    @Test
    @DisplayName("资产不存在抛未找到（404 语义）；非法 sortBy 抛非法参数（422 语义）")
    void impactValidation() {
        assertThatThrownBy(() -> service.getImpact("not-exist", "depth"))
                .isInstanceOf(AssetNotFoundException.class);

        seedAsset("a-root", "dwd_order_di");
        assertThatThrownBy(() -> service.getImpact("a-root", "unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- 导出任务 ----------

    @Test
    @DisplayName("导出 CSV：任务状态 success + 文件生成 + 审计 impact.export")
    void exportCsvSuccess() {
        seedAsset("a-root", "dwd_order_di");
        seedDownstream("a-root", "a-1", "ads_order_1d", "交易域", "内部");

        ExportTaskVO vo = service.exportImpact("a-root", "csv", "u-me");

        assertThat(vo.getStatus()).isEqualTo("success");
        assertThat(vo.getFormat()).isEqualTo("csv");
        assertThat(vo.getOperator()).isEqualTo("u-me");
        assertThat(vo.getAssetId()).isEqualTo("a-root");
        assertThat(vo.getFileRef()).isNotBlank();
        assertThat(vo.getCreatedAt()).isNotNull();
        assertThat(vo.getFinishedAt()).isNotNull();
        // 任务持久化
        assertThat(exportTaskRepository.all()).hasSize(1);
        assertThat(exportTaskRepository.all().get(0).getStatus()).isEqualTo(ExportTaskStatus.SUCCESS);
        // 审计
        assertThat(auditLogRepository.entries()).hasSize(1);
        AuditLogEntry entry = auditLogRepository.entries().get(0);
        assertThat(entry.getAction()).isEqualTo("impact.export");
        assertThat(entry.getOperator()).isEqualTo("u-me");
        assertThat(entry.getObject()).isEqualTo(vo.getId());
    }

    @Test
    @DisplayName("导出 JSON：内容为 ImpactVO JSON 序列化（json-path 解析校验）")
    void exportJsonContent() throws Exception {
        seedAsset("a-root", "dwd_order_di");
        seedDownstream("a-root", "a-1", "ads_order_1d", "交易域", "内部");

        ExportTaskVO vo = service.exportImpact("a-root", "json", "u-me");

        assertThat(vo.getStatus()).isEqualTo("success");
        String content = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get(vo.getFileRef())), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(content).startsWith("{");
        String sortBy = JsonPath.read(content, "$.sortBy");
        assertThat(sortBy).isEqualTo("depth");
        Integer depth = JsonPath.read(content, "$.groups[0].depth");
        assertThat(depth).isEqualTo(1);
        String assetId = JsonPath.read(content, "$.groups[0].items[0].assetId");
        assertThat(assetId).isEqualTo("a-1");
        assertThat(content).contains("\"risk\":\"medium\"");
    }

    @Test
    @DisplayName("幂等：同资产同格式进行中任务复用（不新建任务不重复审计）")
    void exportIdempotentReuseInProgress() {
        seedAsset("a-root", "dwd_order_di");
        ExportTask inProgress = ExportTask.builder().id("task-1").assetId("a-root").format("csv")
                .status(ExportTaskStatus.PENDING).operator("u-other")
                .createdAt(LocalDateTime.now()).build();
        exportTaskRepository.seed(inProgress);

        ExportTaskVO vo = service.exportImpact("a-root", "csv", "u-me");

        assertThat(vo.getId()).isEqualTo("task-1");
        assertThat(vo.getStatus()).isEqualTo("pending");
        assertThat(vo.getOperator()).isEqualTo("u-other");
        assertThat(exportTaskRepository.all()).hasSize(1);
        assertThat(auditLogRepository.entries()).isEmpty();
    }

    @Test
    @DisplayName("已完成任务不阻塞新导出：success 任务后再次导出新建任务")
    void exportAfterCompletedCreatesNew() {
        seedAsset("a-root", "dwd_order_di");
        ExportTask finished = ExportTask.builder().id("task-1").assetId("a-root").format("csv")
                .status(ExportTaskStatus.SUCCESS).fileRef("ref").operator("u-other")
                .createdAt(LocalDateTime.now()).finishedAt(LocalDateTime.now()).build();
        exportTaskRepository.seed(finished);

        ExportTaskVO vo = service.exportImpact("a-root", "csv", "u-me");

        assertThat(vo.getId()).isNotEqualTo("task-1");
        assertThat(vo.getStatus()).isEqualTo("success");
    }

    @Test
    @DisplayName("生成失败：任务标记 failed（202 语义，异常被任务状态承载）")
    void exportFailureMarksFailed() {
        seedAsset("a-root", "dwd_order_di");
        seedDownstream("a-root", "a-1", "ads_order_1d", "交易域", "内部");
        exportFileStorage.failNext(true);

        ExportTaskVO vo = service.exportImpact("a-root", "csv", "u-me");

        assertThat(vo.getStatus()).isEqualTo("failed");
        assertThat(exportTaskRepository.all().get(0).getStatus()).isEqualTo(ExportTaskStatus.FAILED);
    }

    @Test
    @DisplayName("导出校验：资产不存在 404；非法格式 422")
    void exportValidation() {
        assertThatThrownBy(() -> service.exportImpact("not-exist", "csv", "u-me"))
                .isInstanceOf(AssetNotFoundException.class);

        seedAsset("a-root", "dwd_order_di");
        assertThatThrownBy(() -> service.exportImpact("a-root", "xlsx", "u-me"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- 辅助 ----------

    private void seedAsset(String id, String name) {
        assetRepository.seed(Asset.builder().id(id).sourceId("s-1").name(name).type("table")
                .domain("交易域").owner(null).classification("内部").status(AssetStatus.CLAIMED)
                .updatedAt(LocalDateTime.of(2026, 8, 10, 9, 12)).build());
    }

    private void seedDownstream(String from, String to, String name, String domain, String classification) {
        impactRepository.seedNode(to, name, "table", domain, classification);
        impactRepository.seedEdge(LineageEdge.builder().id("e-" + to).fromAssetId(from).toAssetId(to)
                .type(LineageType.SQL).confidence(LineageConfidence.AUTO_HIGH).build());
    }

    private void seedEdgeRaw(String id, String from, String to) {
        impactRepository.seedNode(from, from, "table", null, null);
        impactRepository.seedNode(to, to, "table", null, null);
        impactRepository.seedEdge(LineageEdge.builder().id(id).fromAssetId(from).toAssetId(to)
                .type(LineageType.SQL).confidence(LineageConfidence.AUTO_HIGH).build());
    }

    private void seedChain(String root, int hops) {
        String previous = root;
        for (int i = 1; i <= hops; i++) {
            String current = "chain-" + i;
            impactRepository.seedNode(current, current, "table", null, null);
            impactRepository.seedEdge(LineageEdge.builder().id("e-chain-" + i)
                    .fromAssetId(previous).toAssetId(current)
                    .type(LineageType.SQL).confidence(LineageConfidence.AUTO_HIGH).build());
            previous = current;
        }
    }
}
