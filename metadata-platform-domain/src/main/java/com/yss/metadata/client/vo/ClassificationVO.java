package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 分级分类结果视图对象（冻结 OpenAPI 分类结果响应元素）。
 */
@Getter
@Setter
public class ClassificationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分类结果 id */
    private String id;

    /** 所属资产 id */
    private String assetId;

    /** 所属列 id（列级分类时非空） */
    private String columnId;

    /** 资产名称（查询组合字段，供前端展示） */
    private String assetName;

    /** 列名（查询组合字段，供前端展示） */
    private String columnName;

    /** 分类名（如 敏感-PII / 内部 / 受限） */
    private String name;

    /** 敏感等级 */
    private String level;

    /** 来源（auto/manual） */
    private String source;

    /** 状态（pending/confirmed/corrected） */
    private String status;
}
