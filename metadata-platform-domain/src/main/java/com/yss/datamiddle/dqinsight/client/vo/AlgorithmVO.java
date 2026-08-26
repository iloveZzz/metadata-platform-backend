package com.yss.datamiddle.dqinsight.client.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 分数来源区（冻结 OpenAPI RuleDetail.algorithm，透明可解释）。
 *
 * <p>算法说明：健康分 = Σ(规则权重 × 规则得分)，规则得分 passed=100 / warn=80 / failed|error=0；
 * 权重为 MVP 固定默认系数（OQ-02 / SB-02 已确认，配置化 P1）。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class AlgorithmVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 计算公式（含规则得分映射说明） */
    private String formula;

    /** 默认权重清单（规则名 / 类型 / 权重） */
    private List<RuleWeightVO> weights;
}
