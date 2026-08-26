package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 影响深度分组视图对象（影响分析 data.groups 元素）。
 */
@Getter
@Setter
public class ImpactGroupVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 影响深度（1=直接下游，2=间接第一跳，以此类推） */
    private Integer depth;

    /** 该深度命中项（按 sortBy 排序） */
    private List<ImpactItemVO> items;
}
