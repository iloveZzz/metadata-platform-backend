package com.yss.datamiddle.dqinsight.client.vo;

import com.yss.datamiddle.dqinsight.domain.model.HealthBand;
import com.yss.datamiddle.dqinsight.domain.model.HealthState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 规则明细钻取（冻结 OpenAPI RuleDetail：分数来源区 + 规则明细列表）。
 */
@Getter
@Setter
@NoArgsConstructor
public class RuleDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 资产 ID */
    private String assetId;

    /** 字段名（字段级过滤时返回；null = 资产级） */
    private String fieldName;

    /** 状态 */
    private HealthState state;

    /** 健康分 0~100 */
    private Integer score;

    /** 档位；无结果 / 过期为 null */
    private HealthBand band;

    /** 过期态字段 */
    private boolean expired;

    /** 最近结果时间（ISO 8601） */
    private String lastResultAt;

    /** 结果有效期至（ISO 8601） */
    private String validUntil;

    /** 来源批次号 */
    private String batchNo;

    /** 计算规则版本 */
    private String ruleVersion;

    /** 分数来源区（公式 + 权重 + 算法说明） */
    private AlgorithmVO algorithm;

    /** 规则明细列表 */
    private List<RuleScoreVO> rules;
}
