package com.yss.datamiddle.dqinsight.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/**
 * 规则权重（冻结 OpenAPI RuleWeight；MVP 固定默认系数，OQ-02 / SB-02 已确认，配置化 P1）。
 *
 * <p>默认系数：非空率 0.25 / 格式 0.25 / 唯一性 0.20 / 值域 0.15 / 新鲜度 0.15（权重合计为 1，C22）。</p>
 */
@Getter
@Builder
public class RuleWeight implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 规则名（默认权重清单使用默认规则名，如「非空率」） */
    private final String ruleName;

    /** 规则类型 */
    private final RuleType ruleType;

    /** 权重（固定默认系数，配置化 P1） */
    private final Double weight;
}
