package com.yss.metadata.domain.collector.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 采集产物列值对象（数据架构 asset_column 表）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectedColumn implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 列名 */
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
