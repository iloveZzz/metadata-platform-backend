package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 影响分析命中项视图对象（影响分析页列表元素）。
 */
@Getter
@Setter
public class ImpactItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 下游资产 id */
    private String assetId;

    /** 资产名称 */
    private String name;

    /** 资产类型（table/column/view） */
    private String type;

    /** 数据域 */
    private String domain;

    /** 分级分类 */
    private String classification;

    /** 风险等级（low/medium/high，由分类推导） */
    private String risk;

    /** 影响深度（1=直接下游） */
    private Integer depth;
}
