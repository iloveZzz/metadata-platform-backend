package com.yss.metadata.client.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 字段级影响分析概要指标。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnImpactSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 受波及的独立下游资产总数 */
    private Integer totalAffectedAssets;

    /** 受波及的下游派生字段总数 */
    private Integer totalAffectedColumns;

    /** 下游最深传播层级 */
    private Integer maxDepth;

    /** 是否波及高等级/核心业务资产 */
    private Boolean hasCriticalDownstream;
}
