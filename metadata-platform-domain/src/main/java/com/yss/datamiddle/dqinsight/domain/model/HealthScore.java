package com.yss.datamiddle.dqinsight.domain.model;

import com.yss.datamiddle.dqinsight.domain.service.HealthScoreEngine;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 健康分聚合（HealthScore，质量观测上下文，独立 bounded context 聚合，数据架构 §3/§5）。
 *
 * <p>规则加权计算（健康分 = Σ(规则权重 × 规则得分)，passed=100 / warn=80 / failed|error=0，OQ-02 已确认）
 * 与档位映射（≥90 优 / 75~89 良 / &lt;75 差，OQ-01 已确认）为核心领域规则，落在 Domain
 * （HealthScoreEngine 内部规则引擎，C10）。有效期 validUntil = 工具执行时间 + 30 天（OQ-03 已确认）；
 * 过期展示态由查询投影派生（validUntil &lt; now → expired，与「无结果」独立展示态不混淆，C23）。</p>
 */
@Getter
@Setter
public class HealthScore implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（保存后由持久化分配） */
    private Long id;

    /** 资产 ID（主平台口径） */
    private String assetId;

    /** 字段名（null = 资产级健康分） */
    private String fieldName;

    /** 资产名称快照（来自防腐层，数据架构 §10） */
    private String assetName;

    /** 数据域快照 */
    private String domain;

    /** 资产类型快照 */
    private String assetType;

    /** 健康分 0~100（无结果 / 计算中为 null） */
    private Integer score;

    /** 档位（无结果 / 过期为 null） */
    private HealthBand band;

    /** 状态（已计算 / 过期（派生）/ 无结果 / 计算中） */
    private HealthState state;

    /** 计算规则版本（如 v1，随每次计算递增） */
    private String ruleVersion;

    /** 来源批次 ID */
    private Long batchId;

    /** 计算时间 */
    private Instant computedAt;

    /** 规则通过率（如 '80%'） */
    private String passRate;

    /** 结果有效期至（工具执行时间 + 30 天，OQ-03） */
    private Instant validUntil;

    /** 最近结果时间（工具执行时间快照） */
    private Instant lastResultAt;

    private HealthScore() {
    }

    /**
     * 计算并创建已计算健康分（状态 = 已计算，档位由分值映射）。
     *
     * @param assetId      资产 ID
     * @param fieldName    字段名（null = 资产级）
     * @param assetName    资产名称快照
     * @param domain       数据域快照
     * @param assetType    资产类型快照
     * @param batchId      来源批次 ID
     * @param executionTime 工具执行时间（结果时间，有效期起算，OQ-03）
     * @param ruleVersion  计算规则版本（由调用方按上次版本递增）
     * @param rows         参与计算的规则结果
     */
    public static HealthScore calculate(String assetId, String fieldName, String assetName, String domain,
            String assetType, Long batchId, Instant executionTime, String ruleVersion,
            List<RuleResultRow> rows) {
        HealthScoreEngine engine = new HealthScoreEngine();
        int scoreValue = engine.computeScore(rows);
        HealthScore healthScore = new HealthScore();
        healthScore.assetId = assetId;
        healthScore.fieldName = fieldName;
        healthScore.assetName = assetName;
        healthScore.domain = domain;
        healthScore.assetType = assetType;
        healthScore.batchId = batchId;
        healthScore.score = scoreValue;
        healthScore.band = engine.bandOf(scoreValue);
        healthScore.state = HealthState.OK;
        healthScore.ruleVersion = ruleVersion;
        healthScore.computedAt = Instant.now();
        healthScore.passRate = engine.passRate(rows);
        healthScore.validUntil = executionTime == null
                ? null : executionTime.plus(DQResultBatch.VALIDITY_WINDOW_DAYS, ChronoUnit.DAYS);
        healthScore.lastResultAt = executionTime;
        return healthScore;
    }

    /**
     * 派生当前展示状态：已计算结果超期（validUntil &lt; now）→ 过期独立展示态；其余按持久化状态。
     *
     * @param now 查询时刻
     */
    public HealthState deriveState(Instant now) {
        if (state == HealthState.OK && validUntil != null && now != null && now.isAfter(validUntil)) {
            return HealthState.EXPIRED;
        }
        return state;
    }

    /**
     * 是否过期（validUntil &lt; now）。
     */
    public boolean expiredAt(Instant now) {
        return deriveState(now) == HealthState.EXPIRED;
    }

    public void assignId(Long id) {
        this.id = id;
    }
}
