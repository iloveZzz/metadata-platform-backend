package com.yss.datamiddle.smartgovernance;

import com.yss.datamiddle.smartgovernance.domain.metric.model.ConflictType;
import com.yss.datamiddle.smartgovernance.domain.metric.model.MetricAstDiff;
import com.yss.datamiddle.smartgovernance.domain.metric.service.MetricAstComparator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetricAstComparatorTest {

    private final MetricAstComparator comparator = new MetricAstComparator();

    @Test
    @DisplayName("公式与条件完全一致应判定为同义异名 (SYNONYMOUS_NAME)")
    void testSynonymousName() {
        String formulaA = "sum(order_amount) where status = 1";
        String formulaB = "sum(order_amount) where status = 1";

        MetricAstDiff diff = comparator.compareFormulas(formulaA, formulaB);

        assertThat(diff.getConflictType()).isEqualTo(ConflictType.SYNONYMOUS_NAME);
        assertThat(diff.getSimilarityScore()).isEqualTo(1.0);
        assertThat(diff.getAggMatch()).isTrue();
    }

    @Test
    @DisplayName("聚合相同但 WHERE 过滤条件差异应判定为口径漂移 (FORMULA_DRIFT)")
    void testFormulaDrift() {
        String formulaA = "sum(order_amount) where status in (1, 2)";
        String formulaB = "sum(order_amount) where status = 1";

        MetricAstDiff diff = comparator.compareFormulas(formulaA, formulaB);

        assertThat(diff.getConflictType()).isEqualTo(ConflictType.FORMULA_DRIFT);
        assertThat(diff.getSimilarityScore()).isGreaterThan(0.8);
        assertThat(diff.getAggMatch()).isTrue();
        assertThat(diff.getWhereClauseDiff()).contains("status in (1, 2)");
    }

    @Test
    @DisplayName("聚合函数不同应判定为同名异义 (HOMONYMOUS_MEANING)")
    void testHomonymousMeaning() {
        String formulaA = "sum(user_id) where active = 1";
        String formulaB = "count(distinct user_id) where active = 1";

        MetricAstDiff diff = comparator.compareFormulas(formulaA, formulaB);

        assertThat(diff.getConflictType()).isEqualTo(ConflictType.HOMONYMOUS_MEANING);
        assertThat(diff.getAggMatch()).isFalse();
    }
}
