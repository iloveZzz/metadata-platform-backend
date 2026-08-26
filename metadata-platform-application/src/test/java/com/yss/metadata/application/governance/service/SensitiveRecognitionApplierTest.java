package com.yss.metadata.application.governance.service;

import com.yss.metadata.application.governance.service.support.SensitiveRecognitionApplier;
import com.yss.metadata.application.governance.support.InMemoryClassRuleGateway;
import com.yss.metadata.application.governance.support.InMemoryClassificationGateway;
import com.yss.metadata.domain.collector.model.SavedAssetRef;
import com.yss.metadata.domain.collector.model.SavedColumnRef;
import com.yss.metadata.domain.governance.model.ClassRule;
import com.yss.metadata.domain.governance.model.ClassRuleType;
import com.yss.metadata.domain.governance.model.ClassificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 敏感识别应用器测试（WU-04-02 候选自动识别落库；采集编排 autoClassify 接线）。
 *
 * <p>覆盖：内置/自定义规则命中产候选（source=auto，status=pending）、
 * 幂等（同 asset+column+name 重复采集不重复产候选）、空输入 0、禁用规则排除。</p>
 */
class SensitiveRecognitionApplierTest {

    private InMemoryClassRuleGateway classRuleGateway;
    private InMemoryClassificationGateway classificationGateway;
    private SensitiveRecognitionApplier applier;

    @BeforeEach
    void setUp() {
        classRuleGateway = new InMemoryClassRuleGateway();
        classificationGateway = new InMemoryClassificationGateway();
        applier = new SensitiveRecognitionApplier(classRuleGateway, classificationGateway);
    }

    @Test
    @DisplayName("内置命中：产出待确认候选（source=auto，status=pending），返回新增数")
    void builtinHitCreatesPendingCandidate() {
        int created = applier.apply(Collections.singletonList(
                asset("a-1", "orders", Arrays.asList(
                        column("col-1", "mobile_no", "客户手机号"),
                        column("col-2", "created_at", "创建时间")))));

        assertThat(created).isEqualTo(1);
        assertThat(classificationGateway.store()).hasSize(1);
        com.yss.metadata.domain.governance.model.Classification candidate =
                classificationGateway.store().values().iterator().next();
        assertThat(candidate.getAssetId()).isEqualTo("a-1");
        assertThat(candidate.getColumnId()).isEqualTo("col-1");
        assertThat(candidate.getName()).isEqualTo("敏感-PII");
        assertThat(candidate.getLevel()).isEqualTo("PII");
        assertThat(candidate.getSource()).isEqualTo("auto");
        assertThat(candidate.getStatus()).isEqualTo(ClassificationStatus.PENDING);
    }

    @Test
    @DisplayName("幂等：同 asset+column+name 重复采集不重复产候选（第二次 0 新增）")
    void applyIdempotentOnRepeatedCollect() {
        List<SavedAssetRef> assets = Collections.singletonList(
                asset("a-1", "orders", Collections.singletonList(column("col-1", "id_card", "身份证号"))));

        int first = applier.apply(assets);
        int second = applier.apply(assets);

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        assertThat(classificationGateway.store()).hasSize(1);
    }

    @Test
    @DisplayName("空输入：null/空清单返回 0 且不查询规则")
    void emptyInputReturnsZero() {
        assertThat(applier.apply(null)).isZero();
        assertThat(applier.apply(Collections.emptyList())).isZero();
        assertThat(classificationGateway.store()).isEmpty();
    }

    @Test
    @DisplayName("自定义规则命中 → 敏感/SENSITIVE；禁用规则排除")
    void customRuleHitAndDisabledExcluded() {
        classRuleGateway.seed(rule("r-1", "正则", ClassRuleType.REGEX, "^pay_.*", true));
        classRuleGateway.seed(rule("r-2", "列名", ClassRuleType.COLUMN, "secret", false));

        int created = applier.apply(Collections.singletonList(
                asset("a-1", "pay_flow", Collections.singletonList(column("col-1", "pay_amount", "支付金额")))));

        assertThat(created).isEqualTo(1);
        com.yss.metadata.domain.governance.model.Classification candidate =
                classificationGateway.store().values().iterator().next();
        assertThat(candidate.getName()).isEqualTo("敏感");
        assertThat(candidate.getLevel()).isEqualTo("SENSITIVE");
    }

    @Test
    @DisplayName("多资产多列命中：按列累计候选")
    void multipleAssetsAccumulateCandidates() {
        int created = applier.apply(Arrays.asList(
                asset("a-1", "orders", Collections.singletonList(column("col-1", "phone", "手机"))),
                asset("a-2", "customers", Collections.singletonList(column("col-2", "email", "邮箱")))));

        assertThat(created).isEqualTo(2);
        assertThat(classificationGateway.store()).hasSize(2);
    }

    private SavedAssetRef asset(String assetId, String name, List<SavedColumnRef> columns) {
        return SavedAssetRef.builder().assetId(assetId).name(name).columns(columns).build();
    }

    private SavedColumnRef column(String columnId, String name, String comment) {
        return SavedColumnRef.builder().columnId(columnId).name(name).comment(comment).build();
    }

    private ClassRule rule(String id, String name, ClassRuleType type, String pattern, boolean enabled) {
        return ClassRule.builder().id(id).name(name).type(type).pattern(pattern).enabled(enabled).build();
    }
}
