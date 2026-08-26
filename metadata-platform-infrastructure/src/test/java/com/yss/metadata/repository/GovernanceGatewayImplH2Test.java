package com.yss.metadata.repository;

import com.yss.metadata.domain.governance.gateway.ClassRuleGateway;
import com.yss.metadata.domain.governance.gateway.ClassificationGateway;
import com.yss.metadata.domain.governance.gateway.PropagateTaskGateway;
import com.yss.metadata.domain.governance.model.ClassRule;
import com.yss.metadata.domain.governance.model.ClassRuleType;
import com.yss.metadata.domain.governance.model.Classification;
import com.yss.metadata.domain.governance.model.ClassificationStatus;
import com.yss.metadata.domain.governance.model.PropagateTask;
import com.yss.metadata.domain.governance.model.PropagateTaskStatus;
import com.yss.metadata.infrastructure.convertor.ClassRuleConvertor;
import com.yss.metadata.infrastructure.convertor.ClassificationConvertor;
import com.yss.metadata.infrastructure.convertor.PropagateTaskConvertor;
import com.yss.metadata.repository.entity.AssetColumnPO;
import com.yss.metadata.repository.gateway.impl.ClassRuleGatewayImpl;
import com.yss.metadata.repository.gateway.impl.ClassificationGatewayImpl;
import com.yss.metadata.repository.gateway.impl.PropagateTaskGatewayImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分级分类治理仓储 H2 持久化测试（WU-04-01/02/04；classification/class_rule/propagate_task）。
 */
class GovernanceGatewayImplH2Test extends H2MapperTestSupport {

    private ClassRuleGateway classRuleGateway;
    private ClassificationGateway classificationGateway;
    private PropagateTaskGateway propagateTaskGateway;

    @BeforeEach
    void setUp() {
        ClassRuleRepository classRuleRepository = sqlSession.getMapper(ClassRuleRepository.class);
        ClassificationRepository classificationRepository = sqlSession.getMapper(ClassificationRepository.class);
        PropagateTaskRepository propagateTaskRepository = sqlSession.getMapper(PropagateTaskRepository.class);
        AssetColumnRepository assetColumnRepository = sqlSession.getMapper(AssetColumnRepository.class);

        classRuleGateway = new ClassRuleGatewayImpl(classRuleRepository, Mappers.getMapper(ClassRuleConvertor.class));
        classificationGateway = new ClassificationGatewayImpl(classificationRepository,
                assetColumnRepository, Mappers.getMapper(ClassificationConvertor.class));
        propagateTaskGateway = new PropagateTaskGatewayImpl(propagateTaskRepository,
                Mappers.getMapper(PropagateTaskConvertor.class));
    }

    // ---------- class_rule ----------

    @Test
    @DisplayName("规则保存：插入 + 按 id 查询 + 全量列表（字段完整）")
    void classRuleSaveAndFind() {
        ClassRule saved = classRuleGateway.save(rule("r-1", "手机号正则", ClassRuleType.REGEX, "^1[3-9]\\d{9}$", true));
        classRuleGateway.save(rule("r-2", "列名规则", ClassRuleType.COLUMN, "salary", false));

        assertThat(saved.getId()).isEqualTo("r-1");
        Optional<ClassRule> found = classRuleGateway.findById("r-1");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("手机号正则");
        assertThat(found.get().getType()).isEqualTo(ClassRuleType.REGEX);
        assertThat(found.get().getPattern()).isEqualTo("^1[3-9]\\d{9}$");
        assertThat(found.get().getEnabled()).isTrue();

        assertThat(classRuleGateway.findAll()).extracting(ClassRule::getId)
                .containsExactly("r-1", "r-2");
    }

    @Test
    @DisplayName("规则启停持久化：save 更新 enabled；findEnabled 仅返回启用规则")
    void classRuleTogglePersistsAndFiltersEnabled() {
        classRuleGateway.save(rule("r-1", "正则", ClassRuleType.REGEX, "^a.*", true));
        classRuleGateway.save(rule("r-2", "列名", ClassRuleType.COLUMN, "secret", true));

        ClassRule updated = classRuleGateway.save(rule("r-2", "列名", ClassRuleType.COLUMN, "secret", false));

        assertThat(updated.getEnabled()).isFalse();
        assertThat(classRuleGateway.findById("r-2").orElseThrow(AssertionError::new).getEnabled()).isFalse();
        assertThat(classRuleGateway.findEnabled()).extracting(ClassRule::getId)
                .containsExactly("r-1");
    }

    // ---------- classification ----------

    @Test
    @DisplayName("分类结果保存：插入 + 查询 + 状态更新持久化（确认流转）")
    void classificationSaveAndUpdateStatus() {
        classificationGateway.save(pending("c-1", "a-1", "col-1", "敏感-PII", "PII"));

        Optional<Classification> found = classificationGateway.findById("c-1");
        assertThat(found).isPresent();
        assertThat(found.get().getAssetId()).isEqualTo("a-1");
        assertThat(found.get().getStatus()).isEqualTo(ClassificationStatus.PENDING);

        Classification confirmed = found.get();
        confirmed.confirm();
        classificationGateway.save(confirmed);

        assertThat(classificationGateway.findById("c-1").orElseThrow(AssertionError::new).getStatus())
                .isEqualTo(ClassificationStatus.CONFIRMED);
        assertThat(classificationGateway.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("候选落库幂等：同 asset+column+name 重复跳过，不同 name 新增")
    void saveCandidateIdempotent() {
        classificationGateway.saveCandidate(pending("c-1", "a-1", "col-1", "敏感-PII", "PII"));

        // 同 asset+column+name 重复采集 → 跳过
        classificationGateway.saveCandidate(pending("c-2", "a-1", "col-1", "敏感-PII", "PII"));
        // 不同分类名 → 新增
        classificationGateway.saveCandidate(pending("c-3", "a-1", "col-1", "敏感", "SENSITIVE"));

        assertThat(classificationGateway.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("传播源解析：assetId 直取；列级分类经 asset_column 反查父资产")
    void resolveSourceAssetId() {
        classificationGateway.save(pending("c-1", "a-1", null, "敏感-PII", "PII"));
        assertThat(classificationGateway.resolveSourceAssetId(
                classificationGateway.findById("c-1").orElseThrow(AssertionError::new)))
                .contains("a-1");

        // 列级分类：无 assetId，经 asset_column 反查
        AssetColumnPO column = AssetColumnPO.builder().id("col-9").assetId("a-9")
                .name("phone").type("varchar").build();
        sqlSession.getMapper(AssetColumnRepository.class).insert(column);
        classificationGateway.save(pending("c-2", null, "col-9", "敏感-PII", "PII"));

        assertThat(classificationGateway.resolveSourceAssetId(
                classificationGateway.findById("c-2").orElseThrow(AssertionError::new)))
                .contains("a-9");

        // 列不存在 → 空
        classificationGateway.save(pending("c-3", null, "col-unknown", "敏感-PII", "PII"));
        assertThat(classificationGateway.resolveSourceAssetId(
                classificationGateway.findById("c-3").orElseThrow(AssertionError::new)))
                .isEmpty();
    }

    // ---------- propagate_task ----------

    @Test
    @DisplayName("传播任务保存 + 同 classification+version 幂等查询 + 状态流转持久化")
    void propagateTaskSaveAndQueryByKey() {
        PropagateTask task = PropagateTask.builder()
                .id("t-1").classificationId("c-1").version("c-1#敏感-pii|pii|pending")
                .status(PropagateTaskStatus.PENDING).operator("u-me")
                .createdAt(LocalDateTime.of(2026, 8, 12, 10, 0)).build();
        propagateTaskGateway.save(task);

        Optional<PropagateTask> found = propagateTaskGateway.findByClassificationAndVersion("c-1", "c-1#敏感-pii|pii|pending");
        assertThat(found).isPresent();
        assertThat(found.get().getOperator()).isEqualTo("u-me");

        // 不同 classification / version 查不到
        assertThat(propagateTaskGateway.findByClassificationAndVersion("c-2", "c-1#敏感-pii|pii|pending")).isEmpty();
        assertThat(propagateTaskGateway.findByClassificationAndVersion("c-1", "other-version")).isEmpty();

        // 状态流转持久化（running → success + coverage）
        found.get().setStatus(PropagateTaskStatus.SUCCESS);
        found.get().setCoverage("3");
        found.get().setFinishedAt(LocalDateTime.of(2026, 8, 12, 10, 5));
        propagateTaskGateway.save(found.get());

        PropagateTask reloaded = propagateTaskGateway.findByClassificationAndVersion("c-1", "c-1#敏感-pii|pii|pending")
                .orElseThrow(AssertionError::new);
        assertThat(reloaded.getStatus()).isEqualTo(PropagateTaskStatus.SUCCESS);
        assertThat(reloaded.getCoverage()).isEqualTo("3");
        assertThat(reloaded.getFinishedAt()).isEqualTo(LocalDateTime.of(2026, 8, 12, 10, 5));
    }

    @Test
    @DisplayName("空库查询：findById/findByClassificationAndVersion 返回空（非错误）")
    void emptyStoreQueriesReturnEmpty() {
        assertThat(classRuleGateway.findById("not-exist")).isEmpty();
        assertThat(classRuleGateway.findEnabled()).isEmpty();
        assertThat(classificationGateway.findById("not-exist")).isEmpty();
        assertThat(propagateTaskGateway.findByClassificationAndVersion("c-1", "v")).isEmpty();
    }

    // ---------- 辅助 ----------

    private ClassRule rule(String id, String name, ClassRuleType type, String pattern, boolean enabled) {
        return ClassRule.builder().id(id).name(name).type(type).pattern(pattern).enabled(enabled).build();
    }

    private Classification pending(String id, String assetId, String columnId, String name, String level) {
        return Classification.builder().id(id).assetId(assetId).columnId(columnId)
                .name(name).level(level).source("auto").status(ClassificationStatus.PENDING).build();
    }
}
