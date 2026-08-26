package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 资产字段视图对象（详情聚合字段清单元素）。
 */
@Getter
@Setter
public class AssetColumnVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 列 id */
    private String id;

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
