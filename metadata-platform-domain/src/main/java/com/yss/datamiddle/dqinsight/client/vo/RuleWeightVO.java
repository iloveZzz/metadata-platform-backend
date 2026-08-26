package com.yss.datamiddle.dqinsight.client.vo;

import com.yss.datamiddle.dqinsight.domain.model.RuleType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 规则权重（冻结 OpenAPI RuleWeight；分数来源区展示，OQ-02 / SB-02 已确认固定默认系数，配置化 P1）。
 */
@Getter
@Setter
@NoArgsConstructor
public class RuleWeightVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 规则名 */
    private String ruleName;

    /** 规则类型 */
    private RuleType ruleType;

    /** 权重 */
    private Double weight;
}
