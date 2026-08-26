package com.yss.metadata.client.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 字段级下游爆炸半径 (Blast Radius) 影响分析响应 VO。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnImpactAnalysisVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 源资产 ID */
    private String sourceAssetId;

    /** 源资产名称 */
    private String sourceAssetName;

    /** 源字段 ID */
    private String sourceColumnId;

    /** 源字段名称 */
    private String sourceColumnName;

    /** 影响分析概要指标 */
    private ColumnImpactSummaryVO impactSummary;

    /** 按深度逐层展开的影响层级列表 */
    private List<ColumnImpactLayerVO> impactLayers;
}
