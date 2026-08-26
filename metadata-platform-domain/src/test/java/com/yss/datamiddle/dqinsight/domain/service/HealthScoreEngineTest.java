package com.yss.datamiddle.dqinsight.domain.service;

import com.yss.datamiddle.dqinsight.domain.model.HealthBand;
import com.yss.datamiddle.dqinsight.domain.model.RuleResultRow;
import com.yss.datamiddle.dqinsight.domain.model.RuleStatus;
import com.yss.datamiddle.dqinsight.domain.model.RuleType;
import com.yss.datamiddle.dqinsight.domain.model.RuleWeight;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * 健康分规则加权计算与档位映射领域测试（DQI-SLICE-02-WU1，C22）。
 *
 * <p>公信力关键：权重合计为 1；passed=100 / warn=80 / failed|error=0 映射；档位边界 89/90/74/75；
 * ruleVersion 随每次计算递增；分数可钻取（计算逻辑与分数来源区公式一致，透明可解释）。</p>
 */
class HealthScoreEngineTest {

    private final HealthScoreEngine engine = new HealthScoreEngine();

    private static final Instant EXECUTION_TIME = Instant.parse("2026-08-11T10:00:00Z");

    @Test
    void defaultWeightsSumTo1() {
        double sum = 0d;
        for (RuleWeight weight : engine.defaultWeights()) {
            sum += weight.getWeight();
        }
        assertThat(sum).isCloseTo(1.0d, within(0.000001d));
    }

    @Test
    void defaultWeightsCoverAllFiveRuleTypes() {
        assertThat(engine.weightOf(RuleType.NON_NULL_RATE)).isCloseTo(0.25d, within(0.000001d));
        assertThat(engine.weightOf(RuleType.FORMAT)).isCloseTo(0.25d, within(0.000001d));
        assertThat(engine.weightOf(RuleType.UNIQUENESS)).isCloseTo(0.20d, within(0.000001d));
        assertThat(engine.weightOf(RuleType.VALUE_RANGE)).isCloseTo(0.15d, within(0.000001d));
        assertThat(engine.weightOf(RuleType.FRESHNESS)).isCloseTo(0.15d, within(0.000001d));
    }

    @Test
    void allPassedRulesScore100AndBandGood() {
        List<RuleResultRow> rows = onePerType(RuleStatus.PASSED);
        assertThat(engine.computeScore(rows)).isEqualTo(100);
        assertThat(engine.bandOf(engine.computeScore(rows))).isEqualTo(HealthBand.GOOD);
    }

    @Test
    void allWarnRulesScore80AndBandFair() {
        List<RuleResultRow> rows = onePerType(RuleStatus.WARN);
        assertThat(engine.computeScore(rows)).isEqualTo(80);
        assertThat(engine.bandOf(engine.computeScore(rows))).isEqualTo(HealthBand.FAIR);
    }

    @Test
    void failedAndErrorMapToZeroScore() {
        List<RuleResultRow> rows = onePerType(RuleStatus.FAILED);
        assertThat(engine.computeScore(rows)).isEqualTo(0);
        assertThat(engine.bandOf(0)).isEqualTo(HealthBand.POOR);

        rows = onePerType(RuleStatus.ERROR);
        assertThat(engine.computeScore(rows)).isEqualTo(0);
    }

    @Test
    void ruleScoreMappingIsPassed100Warn80FailedOrError0() {
        assertThat(engine.scoreOf(RuleStatus.PASSED)).isEqualTo(100);
        assertThat(engine.scoreOf(RuleStatus.WARN)).isEqualTo(80);
        assertThat(engine.scoreOf(RuleStatus.FAILED)).isEqualTo(0);
        assertThat(engine.scoreOf(RuleStatus.ERROR)).isEqualTo(0);
    }

    @Test
    void bandBoundaries90Good89Fair75Fair74Poor() {
        assertThat(engine.bandOf(90)).isEqualTo(HealthBand.GOOD);
        assertThat(engine.bandOf(100)).isEqualTo(HealthBand.GOOD);
        assertThat(engine.bandOf(89)).isEqualTo(HealthBand.FAIR);
        assertThat(engine.bandOf(75)).isEqualTo(HealthBand.FAIR);
        assertThat(engine.bandOf(74)).isEqualTo(HealthBand.POOR);
        assertThat(engine.bandOf(0)).isEqualTo(HealthBand.POOR);
    }

    @Test
    void ruleVersionIncrementsPerCalculation() {
        assertThat(engine.nextVersion(null)).isEqualTo("v1");
        assertThat(engine.nextVersion("")).isEqualTo("v1");
        assertThat(engine.nextVersion("v1")).isEqualTo("v2");
        assertThat(engine.nextVersion("v3")).isEqualTo("v4");
        assertThat(engine.nextVersion("v9")).isEqualTo("v10");
        assertThat(engine.nextVersion("v10")).isEqualTo("v11");
    }

    @Test
    void passRateFormatsPassedPercentage() {
        List<RuleResultRow> rows = Arrays.asList(
                row("非空率", RuleType.NON_NULL_RATE, RuleStatus.PASSED),
                row("格式", RuleType.FORMAT, RuleStatus.PASSED),
                row("唯一性", RuleType.UNIQUENESS, RuleStatus.PASSED),
                row("值域", RuleType.VALUE_RANGE, RuleStatus.PASSED),
                row("新鲜度", RuleType.FRESHNESS, RuleStatus.FAILED));
        assertThat(engine.passRate(rows)).isEqualTo("80%");
    }

    @Test
    void missingRuleTypeCountsAsZeroScoreWithWeightKept() {
        // 仅非空率（passed）：0.25 × 100 = 25，其余类型未接入按 0 分计
        List<RuleResultRow> rows = Collections.singletonList(
                row("非空率", RuleType.NON_NULL_RATE, RuleStatus.PASSED));
        assertThat(engine.computeScore(rows)).isEqualTo(25);
    }

    @Test
    void multipleRulesOfSameTypeAreAveragedBeforeWeighting() {
        // 两条格式规则：passed(100) + failed(0) → 平均 50 → 0.25 × 50 = 12.5；其余类型未接入 0 分
        List<RuleResultRow> rows = Arrays.asList(
                row("格式-1", RuleType.FORMAT, RuleStatus.PASSED),
                row("格式-2", RuleType.FORMAT, RuleStatus.FAILED));
        assertThat(engine.computeScore(rows)).isEqualTo(13); // 12.5 四舍五入
    }

    @Test
    void mixedStatusScoreIsTransparentWeightedSum() {
        // 非空率 passed(100) / 格式 warn(80) / 唯一性 failed(0) / 值域 passed(100) / 新鲜度 warn(80)
        // = 0.25×100 + 0.25×80 + 0.20×0 + 0.15×100 + 0.15×80 = 25 + 20 + 0 + 15 + 12 = 72
        List<RuleResultRow> rows = Arrays.asList(
                row("非空率", RuleType.NON_NULL_RATE, RuleStatus.PASSED),
                row("格式", RuleType.FORMAT, RuleStatus.WARN),
                row("唯一性", RuleType.UNIQUENESS, RuleStatus.FAILED),
                row("值域", RuleType.VALUE_RANGE, RuleStatus.PASSED),
                row("新鲜度", RuleType.FRESHNESS, RuleStatus.WARN));
        assertThat(engine.computeScore(rows)).isEqualTo(72);
        assertThat(engine.bandOf(72)).isEqualTo(HealthBand.POOR);
    }

    @Test
    void emptyRowsScoreZero() {
        assertThat(engine.computeScore(null)).isEqualTo(0);
        assertThat(engine.computeScore(Collections.emptyList())).isEqualTo(0);
    }

    private static List<RuleResultRow> onePerType(RuleStatus status) {
        return Arrays.asList(
                row("非空率", RuleType.NON_NULL_RATE, status),
                row("格式", RuleType.FORMAT, status),
                row("唯一性", RuleType.UNIQUENESS, status),
                row("值域", RuleType.VALUE_RANGE, status),
                row("新鲜度", RuleType.FRESHNESS, status));
    }

    private static RuleResultRow row(String ruleName, RuleType ruleType, RuleStatus status) {
        return RuleResultRow.builder()
                .assetId("asset-1")
                .ruleName(ruleName)
                .ruleType(ruleType)
                .status(status)
                .executionTime(EXECUTION_TIME)
                .build();
    }
}
