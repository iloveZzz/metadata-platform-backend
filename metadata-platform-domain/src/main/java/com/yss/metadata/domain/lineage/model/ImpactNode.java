package com.yss.metadata.domain.lineage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 影响分析命中节点（下游全量召回行）。
 *
 * <p>由基础设施递归 CTE 返回（含资产名称/类型/数据域/分类组合字段），
 * 供应用层按深度分组组装 ImpactVO；0 命中以空结构表达（非错误）。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImpactNode implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 下游资产 id */
    private String assetId;

    /** 资产名称（asset.name，组合字段） */
    private String name;

    /** 资产类型（table/column/view） */
    private String type;

    /** 数据域 */
    private String domain;

    /** 分级分类 */
    private String classification;

    /** 影响深度（1=直接下游，2=间接第一跳，以此类推） */
    private int depth;
}
