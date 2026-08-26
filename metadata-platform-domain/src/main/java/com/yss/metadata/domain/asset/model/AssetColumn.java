package com.yss.metadata.domain.asset.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 资产列值对象（数据架构 asset_column 表；详情聚合字段清单）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetColumn implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 列主键（UUID） */
    private String id;

    /** 列名（资产内唯一） */
    private String name;

    /** 列类型 */
    private String type;

    /** 列注释 */
    private String comment;

    /** 是否主键 */
    private Boolean pk;

    /** 物理序号/列顺序（从 1 开始） */
    private Integer ordinalPosition;

    /** 分级分类 */
    private String classification;
}
