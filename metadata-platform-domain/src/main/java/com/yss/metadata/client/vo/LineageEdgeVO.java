package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 血缘边视图对象（冻结 OpenAPI 血缘图 data.edges 元素）。
 */
@Getter
@Setter
public class LineageEdgeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 边 id */
    private String id;

    /** 上游资产 id */
    private String fromAssetId;

    /** 下游资产 id */
    private String toAssetId;

    /** 上游字段 id */
    private String fromColumnId;

    /** 下游字段 id */
    private String toColumnId;

    /** 字段转换表达式 */
    private String transformExpr;

    /** 表达式类型 (DIRECT/COMPUTED/AGGREGATE/MANUAL) */
    private String exprType;

    /** 血缘类型（sql/job/manual） */
    private String type;

    /** 置信度（auto-high/auto-mid/manual-high/low） */
    private String confidence;

    /** 备注 */
    private String remark;
}
