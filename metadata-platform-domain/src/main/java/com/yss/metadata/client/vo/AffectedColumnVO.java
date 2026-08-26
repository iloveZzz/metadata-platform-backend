package com.yss.metadata.client.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 影响分析中受波及字段详情 VO。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AffectedColumnVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 归属资产 ID */
    private String assetId;

    /** 归属资产名称 */
    private String assetName;

    /** 字段 ID */
    private String columnId;

    /** 字段名称 */
    private String columnName;

    /** 数据类型 */
    private String dataType;

    /** 转换 SQL 表达式 */
    private String transformExpr;

    /** 表达式类型 (DIRECT/COMPUTED/AGGREGATE/MANUAL) */
    private String exprType;

    /** 安全分级 (S1/S2/S3/S4) */
    private String classification;
}
