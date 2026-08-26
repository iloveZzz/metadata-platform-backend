package com.yss.datamiddle.dqinsight.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.Instant;

/**
 * 规则级得分快照（dq_rule_detail 行模型，钻取展示透明可解释）。
 *
 * <p>随健康分计算写入：weight 为计算时权重快照（MVP 固定默认系数，P1 配置化后读配置）；
 * 与健康分按 (batch_id, asset_id, field_name) 对齐，钻取无需回查规则结果全表（数据架构 §5/§6）。</p>
 */
@Getter
@Builder
public class RuleScoreSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 来源批次 ID */
    private final Long batchId;

    /** 资产 ID */
    private final String assetId;

    /** 字段名（null = 资产级规则） */
    private final String fieldName;

    /** 规则名 */
    private final String ruleName;

    /** 规则类型 */
    private final RuleType ruleType;

    /** 规则结果状态 */
    private final RuleStatus status;

    /** 计算时权重快照 */
    private final Double weight;

    /** 失败原因 / 说明 */
    private final String failureReason;

    /** 工具执行时间（结果时间） */
    private final Instant executionTime;

    /** 计算规则版本 */
    private final String ruleVersion;

    /**
     * 由规则结果生成快照（权重取该规则类型的默认系数）。
     *
     * @param score 所属健康分（提供批次 / 资产 / 字段 / 版本）
     * @param row   规则结果
     * @param weight 该规则类型权重（引擎默认系数快照）
     */
    public static RuleScoreSnapshot of(HealthScore score, RuleResultRow row, double weight) {
        return RuleScoreSnapshot.builder()
                .batchId(score.getBatchId())
                .assetId(score.getAssetId())
                .fieldName(score.getFieldName())
                .ruleName(row.getRuleName())
                .ruleType(row.getRuleType())
                .status(row.getStatus())
                .weight(weight)
                .failureReason(row.getFailureReason())
                .executionTime(row.getExecutionTime())
                .ruleVersion(score.getRuleVersion())
                .build();
    }
}
