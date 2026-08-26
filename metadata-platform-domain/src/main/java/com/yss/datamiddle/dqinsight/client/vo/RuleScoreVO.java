package com.yss.datamiddle.dqinsight.client.vo;

import com.yss.datamiddle.dqinsight.domain.model.RuleStatus;
import com.yss.datamiddle.dqinsight.domain.model.RuleType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 规则明细行（冻结 OpenAPI RuleDetail.rules：规则名 / 权重 / 结果 / 失败原因 / 工具结果时间）。
 */
@Getter
@Setter
@NoArgsConstructor
public class RuleScoreVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 字段名（空 = 资产级规则） */
    private String fieldName;

    /** 规则名 */
    private String ruleName;

    /** 规则类型 */
    private RuleType ruleType;

    /** 规则结果状态 */
    private RuleStatus status;

    /** 失败原因 / 说明 */
    private String failureReason;

    /** 该规则权重（固定默认系数，OQ-02 已确认） */
    private Double weight;

    /** 工具结果时间（ISO 8601） */
    private String toolTime;
}
