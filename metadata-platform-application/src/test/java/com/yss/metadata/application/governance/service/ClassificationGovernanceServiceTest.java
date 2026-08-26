package com.yss.metadata.application.governance.service;

import com.yss.metadata.application.asset.support.InMemoryAssetRepository;
import com.yss.metadata.application.governance.service.impl.ClassificationGovernanceServiceImpl;
import com.yss.metadata.application.governance.support.InMemoryClassRuleGateway;
import com.yss.metadata.application.governance.support.InMemoryClassificationGateway;
import com.yss.metadata.application.governance.support.InMemoryPropagateTaskGateway;
import com.yss.metadata.application.lineage.support.InMemoryAuditLogRepository;
import com.yss.metadata.application.lineage.support.InMemoryImpactAnalysisRepository;
import com.yss.metadata.client.dto.cmd.ClassRuleCmd;
import com.yss.metadata.client.vo.ClassRuleVO;
import com.yss.metadata.client.vo.ClassificationOverviewVO;
import com.yss.metadata.client.vo.ClassificationVO;
import com.yss.metadata.client.vo.PropagateTaskVO;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetStatus;
import com.yss.metadata.domain.audit.model.AuditLogEntry;
import com.yss.metadata.domain.governance.exception.ClassificationNotFoundException;
import com.yss.metadata.domain.governance.exception.ClassRuleNotFoundException;
import com.yss.metadata.domain.governance.model.ClassRule;
import com.yss.metadata.domain.governance.model.ClassRuleType;
import com.yss.metadata.domain.governance.model.Classification;
import com.yss.metadata.domain.governance.model.ClassificationStatus;
import com.yss.metadata.domain.governance.model.PropagateTask;
import com.yss.metadata.domain.governance.model.PropagateTaskStatus;
import com.yss.metadata.domain.lineage.gateway.ImpactAnalysisRepository;
import com.yss.metadata.domain.lineage.model.ImpactNode;
import com.yss.metadata.domain.lineage.model.LineageConfidence;
import com.yss.metadata.domain.lineage.model.LineageEdge;
import com.yss.metadata.domain.lineage.model.LineageType;
import com.yss.metadata.application.governance.service.convertor.GovernanceAppConvertor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 分级分类治理应用服务测试（WU-04-01 规则 / WU-04-02 结果 / WU-04-03 传播 / WU-04-04 任务）。
 *
 * <p>覆盖：概览组合 VO、规则创建（默认启用 + 审计）、规则启停（幂等 + 审计 + 404）、
 * 候选确认（幂等）/修正、传播（仅覆盖空分类 + 源资产兜底 + coverage 可核验 + 审计 +
 * 同版本幂等复用 + 失败任务状态承载 + 404/422 语义）。</p>
 */
class ClassificationGovernanceServiceTest {

    private InMemoryClassRuleGateway classRuleGateway;
    private InMemoryClassificationGateway classificationGateway;
    private InMemoryPropagateTaskGateway propagateTaskGateway;
    private InMemoryImpactAnalysisRepository impactAnalysisRepository;
    private InMemoryAssetRepository assetRepository;
    private InMemoryAuditLogRepository auditLogRepository;
    private ClassificationGovernanceService service;

    @BeforeEach
    void setUp() {
        classRuleGateway = new InMemoryClassRuleGateway();
        classificationGateway = new InMemoryClassificationGateway();
        propagateTaskGateway = new InMemoryPropagateTaskGateway();
        impactAnalysisRepository = new InMemoryImpactAnalysisRepository();
        assetRepository = new InMemoryAssetRepository();
        auditLogRepository = new InMemoryAuditLogRepository();
        service = new ClassificationGovernanceServiceImpl(
                classRuleGateway, classificationGateway, propagateTaskGateway,
                impactAnalysisRepository, assetRepository, auditLogRepository,
                org.mapstruct.factory.Mappers.getMapper(GovernanceAppConvertor.class));
    }

    // ---------- WU-04-01 规则 ----------

    @Test
    @DisplayName("概览返回组合 VO：规则 + 结果一次返回（0 数据空结构非错误）")
    void overviewCombinesRulesAndResults() {
        ClassificationOverviewVO empty = service.getOverview();
        assertThat(empty.getRules()).isEmpty();
        assertThat(empty.getResults()).isEmpty();

        classRuleGateway.seed(rule("r-1", "内置-手机号", ClassRuleType.BUILTIN, "phone", true));
        seedAsset("a-1", "orders", null);
        seedColumn("a-1", "col-1", "mobile_no");
        classificationGateway.seed(pending("c-1", "a-1", "col-1", "敏感-PII", "PII"));

        ClassificationOverviewVO vo = service.getOverview();

        assertThat(vo.getRules()).hasSize(1);
        assertThat(vo.getRules().get(0).getName()).isEqualTo("内置-手机号");
        assertThat(vo.getRules().get(0).getType()).isEqualTo("builtin");
        assertThat(vo.getResults()).hasSize(1);
        assertThat(vo.getResults().get(0).getName()).isEqualTo("敏感-PII");
        assertThat(vo.getResults().get(0).getStatus()).isEqualTo("pending");
        // 组合展示字段（F2 修复）：assetName 经资产解析；columnName 经列清单匹配
        assertThat(vo.getResults().get(0).getAssetName()).isEqualTo("orders");
        assertThat(vo.getResults().get(0).getColumnName()).isEqualTo("mobile_no");
    }

    @Test
    @DisplayName("创建规则：默认启用（enabled 缺省 true）+ 审计 classify.rule")
    void createRuleDefaultsEnabledAndAudits() {
        ClassRuleCmd cmd = new ClassRuleCmd();
        cmd.setName("手机号正则");
        cmd.setType(ClassRuleType.REGEX);
        cmd.setPattern("^1[3-9]\\d{9}$");

        ClassRuleVO vo = service.createRule(cmd, "u-me");

        assertThat(vo.getId()).isNotBlank();
        assertThat(vo.getName()).isEqualTo("手机号正则");
        assertThat(vo.getType()).isEqualTo("regex");
        assertThat(vo.getEnabled()).isTrue();

        assertThat(auditLogRepository.entries()).hasSize(1);
        AuditLogEntry entry = auditLogRepository.entries().get(0);
        assertThat(entry.getAction()).isEqualTo("classify.rule");
        assertThat(entry.getOperator()).isEqualTo("u-me");
        assertThat(entry.getObject()).isEqualTo(vo.getId());
    }

    @Test
    @DisplayName("创建规则：enabled=false 显式禁用生效")
    void createRuleHonorsExplicitDisabled() {
        ClassRuleCmd cmd = new ClassRuleCmd();
        cmd.setName("字典规则");
        cmd.setType(ClassRuleType.DICTIONARY);
        cmd.setPattern("工号,绩效");
        cmd.setEnabled(Boolean.FALSE);

        ClassRuleVO vo = service.createRule(cmd, "u-me");

        assertThat(vo.getEnabled()).isFalse();
    }

    @Test
    @DisplayName("规则启停：翻转 enabled + 审计 classify.rule.status（result 承载状态）")
    void toggleRuleFlipsEnabledAndAudits() {
        classRuleGateway.seed(rule("r-1", "正则", ClassRuleType.REGEX, "^a.*", true));

        ClassRuleVO disabled = service.toggleRule("r-1", false, "u-me");
        assertThat(disabled.getEnabled()).isFalse();

        ClassRuleVO enabled = service.toggleRule("r-1", true, "u-me");
        assertThat(enabled.getEnabled()).isTrue();

        assertThat(auditLogRepository.entries()).hasSize(2);
        assertThat(auditLogRepository.entries()).extracting(AuditLogEntry::getAction)
                .containsExactly("classify.rule.status", "classify.rule.status");
        assertThat(auditLogRepository.entries()).extracting(AuditLogEntry::getResult)
                .containsExactly("disabled", "enabled");
    }

    @Test
    @DisplayName("规则启停：规则不存在抛未找到（404 语义）")
    void toggleRuleNotFoundThrows() {
        assertThatThrownBy(() -> service.toggleRule("not-exist", false, "u-me"))
                .isInstanceOf(ClassRuleNotFoundException.class);
        assertThat(auditLogRepository.entries()).isEmpty();
    }

    // ---------- WU-04-02 结果 ----------

    @Test
    @DisplayName("候选确认：pending → confirmed（幂等重复确认无操作）")
    void confirmCandidateTransitions() {
        classificationGateway.seed(pending("c-1", "a-1", "col-1", "敏感-PII", "PII"));

        ClassificationVO confirmed = service.confirm("c-1", null);

        assertThat(confirmed.getStatus()).isEqualTo("confirmed");

        ClassificationVO again = service.confirm("c-1", null);
        assertThat(again.getStatus()).isEqualTo("confirmed");
    }

    @Test
    @DisplayName("候选修正：correctedName 覆盖分类名并流转已修正")
    void correctCandidateOverridesName() {
        classificationGateway.seed(pending("c-1", "a-1", "col-1", "敏感-PII", "PII"));

        ClassificationVO corrected = service.confirm("c-1", "内部受限");

        assertThat(corrected.getName()).isEqualTo("内部受限");
        assertThat(corrected.getStatus()).isEqualTo("corrected");
        // 持久化已流转
        assertThat(classificationGateway.store().get("c-1").getStatus())
                .isEqualTo(ClassificationStatus.CORRECTED);
    }

    @Test
    @DisplayName("结果确认：分类不存在抛未找到（404 语义）")
    void confirmNotFoundThrows() {
        assertThatThrownBy(() -> service.confirm("not-exist", null))
                .isInstanceOf(ClassificationNotFoundException.class);
    }

    // ---------- WU-04-03 传播 ----------

    @Test
    @DisplayName("传播成功：下游仅覆盖空分类 + 源资产兜底 + coverage 可核验 + 审计")
    void propagateWritesDownstreamAndSource() {
        classificationGateway.seed(pending("c-1", "a-root", "col-1", "敏感-PII", "PII"));
        seedAsset("a-root", "dwd_order_di", null);
        seedAsset("a-1", "ads_order_1d", null);
        seedAsset("a-2", "bi_report_2d", "内部"); // 已有分类不覆盖
        seedEdge("a-root", "a-1");
        seedEdge("a-1", "a-2");

        PropagateTaskVO vo = service.propagate("c-1", "u-me");

        assertThat(vo.getStatus()).isEqualTo("success");
        assertThat(vo.getClassificationId()).isEqualTo("c-1");
        assertThat(vo.getOperator()).isEqualTo("u-me");
        // coverage = 源兜底(a-root) + 下游空分类(a-1)；a-2 已有分类不计
        assertThat(vo.getCoverage()).isEqualTo("2");
        assertThat(assetRepository.store().get("a-root").getClassification()).isEqualTo("敏感-PII");
        assertThat(assetRepository.store().get("a-1").getClassification()).isEqualTo("敏感-PII");
        assertThat(assetRepository.store().get("a-2").getClassification()).isEqualTo("内部");

        assertThat(auditLogRepository.entries()).hasSize(1);
        AuditLogEntry entry = auditLogRepository.entries().get(0);
        assertThat(entry.getAction()).isEqualTo("classify.propagate");
        assertThat(entry.getOperator()).isEqualTo("u-me");
        assertThat(entry.getResult()).isEqualTo("success");
        assertThat(entry.getObject()).isEqualTo(vo.getId());
    }

    @Test
    @DisplayName("传播幂等：同 classification+version 既有任务复用，不重复执行不重复审计")
    void propagateIdempotentReusesTask() {
        classificationGateway.seed(pending("c-1", "a-root", "col-1", "敏感-PII", "PII"));
        seedAsset("a-root", "dwd_order_di", null);
        seedEdge("a-root", "a-1");
        seedAsset("a-1", "ads_order_1d", null);

        PropagateTaskVO first = service.propagate("c-1", "u-me");
        PropagateTaskVO second = service.propagate("c-1", "u-other");

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getOperator()).isEqualTo("u-me"); // 既有任务保留首触发人
        // 仅首次执行产生任务（初始 save + running + 终态均为同一条）+ 一条审计
        assertThat(propagateTaskGateway.store()).hasSize(1);
        assertThat(auditLogRepository.entries()).hasSize(1);
    }

    @Test
    @DisplayName("传播失败后同版本重试：原地复用任务行重新执行（F1/F9 修复，同键保持单行）")
    void propagateRetryAfterFailureReusesTaskRow() {
        classificationGateway.seed(pending("c-1", "a-root", "col-1", "敏感-PII", "PII"));
        seedAsset("a-root", "dwd_order_di", null);
        seedEdge("a-root", "a-1");
        seedAsset("a-1", "ads_order_1d", null);

        // 首次：下游召回抛异常 → failed
        service = new ClassificationGovernanceServiceImpl(
                classRuleGateway, classificationGateway, propagateTaskGateway,
                new ThrowingImpactAnalysisRepository(), assetRepository, auditLogRepository,
                org.mapstruct.factory.Mappers.getMapper(GovernanceAppConvertor.class));
        PropagateTaskVO failed = service.propagate("c-1", "u-me");
        assertThat(failed.getStatus()).isEqualTo("failed");

        // 恢复下游后重试：原地复用同一任务行 → 同一 id，状态 success
        service = new ClassificationGovernanceServiceImpl(
                classRuleGateway, classificationGateway, propagateTaskGateway,
                impactAnalysisRepository, assetRepository, auditLogRepository,
                org.mapstruct.factory.Mappers.getMapper(GovernanceAppConvertor.class));
        PropagateTaskVO retried = service.propagate("c-1", "u-other");

        assertThat(retried.getId()).isEqualTo(failed.getId());
        assertThat(retried.getStatus()).isEqualTo("success");
        // 同键保持单行（F9：避免重复行导致「同版本只跑一次」失效）
        assertThat(propagateTaskGateway.store()).hasSize(1);
        assertThat(auditLogRepository.entries()).hasSize(2);

        // 第 3 次触发：success 任务幂等复用，不新增行不重复审计（F9 回归）
        PropagateTaskVO third = service.propagate("c-1", "u-me");
        assertThat(third.getId()).isEqualTo(failed.getId());
        assertThat(propagateTaskGateway.store()).hasSize(1);
        assertThat(auditLogRepository.entries()).hasSize(2);
    }

    @Test
    @DisplayName("修正后重新传播：分类内容变化 → 版本签名变化 → 产生新任务（F8）")
    void propagateAfterCorrectCreatesNewTask() {
        classificationGateway.seed(pending("c-1", "a-root", "col-1", "敏感-PII", "PII"));
        seedAsset("a-root", "dwd_order_di", null);
        seedEdge("a-root", "a-1");
        seedAsset("a-1", "ads_order_1d", null);

        PropagateTaskVO first = service.propagate("c-1", "u-me");
        assertThat(first.getVersion()).isNotBlank();

        service.confirm("c-1", "内部受限");

        PropagateTaskVO second = service.propagate("c-1", "u-me");

        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(second.getVersion()).isNotEqualTo(first.getVersion());
        assertThat(propagateTaskGateway.store()).hasSize(2);
    }

    @Test
    @DisplayName("传播失败：异常不抛出，任务状态 failed + coverage 0（审计 result=failed）")
    void propagateFailureMarksTaskFailed() {
        classificationGateway.seed(pending("c-1", "a-root", "col-1", "敏感-PII", "PII"));
        seedAsset("a-root", "dwd_order_di", null);
        // 下游召回抛异常 → 任务 failed
        service = new ClassificationGovernanceServiceImpl(
                classRuleGateway, classificationGateway, propagateTaskGateway,
                new ThrowingImpactAnalysisRepository(), assetRepository, auditLogRepository,
                org.mapstruct.factory.Mappers.getMapper(GovernanceAppConvertor.class));

        PropagateTaskVO vo = service.propagate("c-1", "u-me");

        assertThat(vo.getStatus()).isEqualTo("failed");
        assertThat(vo.getCoverage()).isEqualTo("0");
        assertThat(auditLogRepository.entries()).hasSize(1);
        assertThat(auditLogRepository.entries().get(0).getResult()).isEqualTo("failed");
    }

    @Test
    @DisplayName("传播校验：分类不存在 404；分类未关联资产 422")
    void propagateValidation() {
        assertThatThrownBy(() -> service.propagate("not-exist", "u-me"))
                .isInstanceOf(ClassificationNotFoundException.class);

        classificationGateway.seed(Classification.builder()
                .id("c-1").assetId(" ").columnId(null)
                .name("敏感-PII").level("PII").source("auto")
                .status(ClassificationStatus.PENDING).build());
        assertThatThrownBy(() -> service.propagate("c-1", "u-me"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未关联资产");
    }

    // ---------- 辅助 ----------

    private void seedEdge(String from, String to) {
        impactAnalysisRepository.seedEdge(LineageEdge.builder().id("e-" + from + "-" + to)
                .fromAssetId(from).toAssetId(to)
                .type(LineageType.SQL).confidence(LineageConfidence.AUTO_HIGH).build());
    }

    private void seedAsset(String id, String name, String classification) {
        assetRepository.seed(Asset.builder().id(id).sourceId("s-1").name(name).type("table")
                .domain("交易域").owner(null).classification(classification).status(AssetStatus.CLAIMED)
                .updatedAt(LocalDateTime.of(2026, 8, 10, 9, 12)).build());
    }

    private void seedColumn(String assetId, String columnId, String name) {
        assetRepository.seedColumns(assetId, Collections.singletonList(
                com.yss.metadata.domain.asset.model.AssetColumn.builder()
                        .id(columnId).name(name).type("varchar").build()));
    }

    private ClassRule rule(String id, String name, ClassRuleType type, String pattern, boolean enabled) {
        return ClassRule.builder().id(id).name(name).type(type).pattern(pattern).enabled(enabled).build();
    }

    private Classification pending(String id, String assetId, String columnId, String name, String level) {
        return Classification.builder().id(id).assetId(assetId).columnId(columnId)
                .name(name).level(level).source("auto").status(ClassificationStatus.PENDING).build();
    }

    /** 下游召回抛异常的端口替身（验证传播失败任务状态承载）。 */
    private static final class ThrowingImpactAnalysisRepository implements ImpactAnalysisRepository {
        @Override
        public List<ImpactNode> findDownstream(String assetId, int maxDepth) {
            throw new IllegalStateException("downstream query failed");
        }
    }
}
