package com.yss.datamiddle.dqinsight.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * 规则结果（批次内逐条规则判定，RuleResultRow）。
 *
 * <p>field_name 空 / null = 资产级规则；assetId 来自 CSV 每行 asset_id 或 JSON 顶层 assetId。</p>
 */
@Getter
@Setter
@Builder
public class RuleResultRow implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 资产 ID（主平台口径） */
    private String assetId;

    /** 字段名；空 / null = 资产级规则 */
    private String fieldName;

    /** 规则名 */
    private String ruleName;

    /** 规则类型 */
    private RuleType ruleType;

    /** 规则结果状态 */
    private RuleStatus status;

    /** 失败原因 / 说明 */
    private String failureReason;

    /** 工具执行时间（结果时间，有效期起算） */
    private Instant executionTime;
}
