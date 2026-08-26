package com.yss.datamiddle.dqinsight.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 健康分有效期 / 过期流转领域测试（DQI-SLICE-02-WU2，C23）。
 *
 * <p>validUntil = 工具执行时间 + 30 天（OQ-03 默认窗口，配置化 P1）；超期系统自动流转为过期独立展示态
 * （expired=true + validUntil），与「无结果」独立展示态不混淆；重新接入新批次重算后恢复档位。</p>
 */
class HealthScoreExpiryTest {

    private static final Instant EXECUTION_TIME = Instant.parse("2026-08-11T10:00:00Z");

    @Test
    void validUntilIsExecutionTimePlusThirtyDays() {
        HealthScore score = HealthScore.calculate("asset-1", null, "用户表", "交易域", "table",
                1L, EXECUTION_TIME, "v1", Collections.emptyList());

        assertThat(score.getValidUntil())
                .isEqualTo(EXECUTION_TIME.plus(DQResultBatch.VALIDITY_WINDOW_DAYS, ChronoUnit.DAYS));
        assertThat(score.getLastResultAt()).isEqualTo(EXECUTION_TIME);
    }

    @Test
    void expiredStateIsDerivedWhenValidUntilPassed() {
        Instant pastExecution = Instant.now().minus(31, ChronoUnit.DAYS);
        HealthScore score = HealthScore.calculate("asset-1", null, "用户表", "交易域", "table",
                1L, pastExecution, "v1", Collections.emptyList());

        assertThat(score.deriveState(Instant.now())).isEqualTo(HealthState.EXPIRED);
        assertThat(score.expiredAt(Instant.now())).isTrue();
    }

    @Test
    void notExpiredBeforeValidUntil() {
        HealthScore score = HealthScore.calculate("asset-1", null, "用户表", "交易域", "table",
                1L, EXECUTION_TIME, "v1", Collections.emptyList());

        assertThat(score.deriveState(Instant.now())).isEqualTo(HealthState.OK);
        assertThat(score.expiredAt(Instant.now())).isFalse();
    }

    @Test
    void recomputeAfterReIngestionRestoresComputedBand() {
        // 第一次计算（超期批次）
        HealthScore expired = HealthScore.calculate("asset-1", null, "用户表", "交易域", "table",
                1L, Instant.now().minus(31, ChronoUnit.DAYS), "v1", Collections.emptyList());
        assertThat(expired.deriveState(Instant.now())).isEqualTo(HealthState.EXPIRED);

        // 重新接入新批次 → 重算（新版本）→ 过期态恢复为已计算档位
        HealthScore recalculated = HealthScore.calculate("asset-1", null, "用户表", "交易域", "table",
                2L, Instant.now(), "v2", Collections.emptyList());
        assertThat(recalculated.getRuleVersion()).isEqualTo("v2");
        assertThat(recalculated.deriveState(Instant.now())).isEqualTo(HealthState.OK);
        assertThat(recalculated.getState()).isEqualTo(HealthState.OK);
    }
}
