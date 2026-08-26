package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 影响分析视图对象（冻结 OpenAPI GET /api/assets/{id}/impact-analysis 响应 data）。
 *
 * <p>下游全量召回并按影响深度分组；0 影响以空 groups 表达（非错误）。</p>
 */
@Getter
@Setter
public class ImpactVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 深度分组（按深度升序） */
    private List<ImpactGroupVO> groups;

    /** 排序键（depth/domain/risk） */
    private String sortBy;
}
