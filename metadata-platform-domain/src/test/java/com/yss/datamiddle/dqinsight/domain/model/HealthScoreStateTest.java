package com.yss.datamiddle.dqinsight.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 健康分状态 / 展示态领域测试（DQI-SLICE-02-WU1/WU2）。
 *
 * <p>过期态（expired）与「无结果」（noresult）为独立展示态不混淆（C23）；计算中（calculating）为独立状态；
 * 档位阈值 ≥90 优 / 75~89 良 / &lt;75 差（OQ-01）。</p>
 */
class HealthScoreStateTest {

    private static final Instant EXECUTION_TIME = Instant.parse("2026-08-11T10:00:00Z");

    @Test
    void calculateProducesOkStateWithBandAndRuleVersion() {
        HealthScore score = HealthScore.calculate("asset-1", null, "用户表", "交易域", "table",
                1L, EXECUTION_TIME, "v1", Collections.emptyList());

        assertThat(score.getState()).isEqualTo(HealthState.OK);
        assertThat(score.getBand()).isEqualTo(HealthBand.POOR); // 无规则 → 0 分 → 差
        assertThat(score.getRuleVersion()).isEqualTo("v1");
        assertThat(score.getScore()).isEqualTo(0);
    }

    @Test
    void expiredAndNoresultAreDistinctDisplayStates() {
        assertThat(HealthState.EXPIRED).isNotEqualTo(HealthState.NORESULT);
        assertThat(HealthState.EXPIRED.getCode()).isEqualTo("expired");
        assertThat(HealthState.NORESULT.getCode()).isEqualTo("noresult");
        assertThat(HealthState.OK.getCode()).isEqualTo("ok");
        assertThat(HealthState.CALCULATING.getCode()).isEqualTo("calculating");
    }

    @Test
    void calculatingStateIsNotDerivedAsExpired() {
        HealthScore score = HealthScore.calculate("asset-1", null, "用户表", "交易域", "table",
                1L, EXECUTION_TIME, "v1", Collections.emptyList());
        score.setState(HealthState.CALCULATING);
        score.setValidUntil(Instant.now().minus(1, ChronoUnit.DAYS));

        // 计算中状态维持，不被过期派生覆盖
        assertThat(score.deriveState(Instant.now())).isEqualTo(HealthState.CALCULATING);
        assertThat(score.expiredAt(Instant.now())).isFalse();
    }

    @Test
    void noresultStateIsNeverMappedToBand() {
        HealthScore score = HealthScore.calculate("asset-1", "name", "用户表", "交易域", "table",
                1L, EXECUTION_TIME, "v1", Collections.emptyList());
        score.setState(HealthState.NORESULT);
        score.setScore(null);
        score.setBand(null);

        // 无结果独立展示态：不归入档位，且即使 validUntil 为空也不派生为过期
        assertThat(score.deriveState(Instant.now())).isEqualTo(HealthState.NORESULT);
        assertThat(score.getBand()).isNull();
    }
}
