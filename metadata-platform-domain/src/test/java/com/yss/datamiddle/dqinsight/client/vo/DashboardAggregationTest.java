package com.yss.datamiddle.dqinsight.client.vo;

import com.yss.datamiddle.dqinsight.domain.model.DashboardSort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * 仪表盘聚合领域规则测试（DQI-SLICE-03-WU1，SB-07 覆盖率口径 + C28 聚合语义）。
 *
 * <p>公信力 / 口径关键：已接入 = 优 + 良 + 差 + 过期（含过期）；noResult = 目标 − 已接入（钳制 ≥ 0）；
 * 覆盖率 = 已接入 ÷ 目标 × 100（目标 = 0 不除零，按 0 表达）；bandDistribution 合计与已接入一致。</p>
 */
class DashboardAggregationTest {

    @Test
    void computeSumsIngestedIncludingExpired() {
        DashboardStatsVO stats = DashboardStatsVO.compute(3, 2, 1, 1, 1, 10);

        assertThat(stats.getIngestedAssetCount()).isEqualTo(7);
        assertThat(stats.getBandDistribution().getGood()).isEqualTo(3);
        assertThat(stats.getBandDistribution().getFair()).isEqualTo(2);
        assertThat(stats.getBandDistribution().getPoor()).isEqualTo(1);
        assertThat(stats.getBandDistribution().getExpired()).isEqualTo(1);
    }

    @Test
    void coverageIsIngestedDividedByTarget() {
        DashboardStatsVO stats = DashboardStatsVO.compute(3, 2, 1, 1, 1, 10);

        assertThat(stats.getCoverage()).isCloseTo(70.0f, within(0.001f));
        assertThat(stats.getTargetAssetCount()).isEqualTo(10);
    }

    @Test
    void noResultIsTargetMinusIngested() {
        DashboardStatsVO stats = DashboardStatsVO.compute(3, 2, 1, 1, 1, 10);

        assertThat(stats.getBandDistribution().getNoResult()).isEqualTo(3);
        // 分布合计 = 已接入 + 无结果 = 目标资产数
        assertThat(stats.getBandDistribution().getGood() + stats.getBandDistribution().getFair()
                + stats.getBandDistribution().getPoor() + stats.getBandDistribution().getExpired()
                + stats.getBandDistribution().getNoResult()).isEqualTo(10);
    }

    @Test
    void noResultClampedToZeroWhenIngestedExceedsTarget() {
        // 快照漂移（已接入 > 目标）：无结果不出现负数
        DashboardStatsVO stats = DashboardStatsVO.compute(5, 4, 3, 2, 3, 10);

        assertThat(stats.getBandDistribution().getNoResult()).isZero();
        assertThat(stats.getIngestedAssetCount()).isEqualTo(14);
    }

    @Test
    void zeroTargetYieldsZeroCoverageWithoutDivisionByZero() {
        DashboardStatsVO stats = DashboardStatsVO.compute(0, 0, 0, 0, 0, 0);

        assertThat(stats.getCoverage()).isZero();
        assertThat(stats.getTargetAssetCount()).isZero();
        assertThat(stats.getBandDistribution().getNoResult()).isZero();
    }

    @Test
    void lowScoreCountPassesThroughExcludingExpired() {
        // 低分 = 差档非过期行；过期差行不重复计入（过期计入 ingested 与 expired 分布）
        DashboardStatsVO stats = DashboardStatsVO.compute(1, 1, 2, 1, 2, 8);

        assertThat(stats.getLowScoreAssetCount()).isEqualTo(2);
        assertThat(stats.getIngestedAssetCount()).isEqualTo(5);
    }

    @Test
    void sortCodesRoundTrip() {
        assertThat(DashboardSort.fromCodeOrNull("score")).isEqualTo(DashboardSort.SCORE);
        assertThat(DashboardSort.fromCodeOrNull("lastResultAt")).isEqualTo(DashboardSort.LAST_RESULT_AT);
        assertThat(DashboardSort.fromCodeOrNull("name")).isEqualTo(DashboardSort.NAME);
        assertThat(DashboardSort.fromCodeOrNull(null)).isNull();
        assertThat(DashboardSort.fromCodeOrNull("unknown")).isNull();
        assertThat(DashboardSort.fromCodeOrNull("")).isNull();
    }
}
