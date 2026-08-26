package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 分类规则视图对象（冻结 OpenAPI 分类规则响应元素）。
 */
@Getter
@Setter
public class ClassRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 规则 id */
    private String id;

    /** 规则名 */
    private String name;

    /** 规则类型（builtin/regex/column/dictionary） */
    private String type;

    /** 匹配模式（正则表达式 / 列名关键字 / 字典引用） */
    private String pattern;

    /** 是否启用 */
    private Boolean enabled;
}
