package com.yss.metadata.client.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 字段级血缘节点视图对象。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnLineageNodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 归属资产 ID */
    private String assetId;

    /** 归属资产名称 */
    private String assetName;

    /** 物理表名 */
    private String tableName;

    /** 字段 ID */
    private String columnId;

    /** 字段名称 */
    private String columnName;

    /** 数据类型 */
    private String dataType;

    /** 敏感级别 (S1/S2/S3/S4 等) */
    private String classification;

    /** 是否主键 */
    private Boolean isPrimaryKey;
}
