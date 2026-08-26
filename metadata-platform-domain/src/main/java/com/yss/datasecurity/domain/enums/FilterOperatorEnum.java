package com.yss.datasecurity.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 过滤条件操作符枚举
 */
@Getter
@RequiredArgsConstructor
public enum FilterOperatorEnum {
    CONTAINS("CONTAINS", "包含"),
    NOT_CONTAINS("NOT_CONTAINS", "不包含"),
    PREFIX("PREFIX", "前缀匹配"),
    SUFFIX("SUFFIX", "后缀匹配"),
    IN("IN", "属于"),
    IN_LIST("IN_LIST", "在列表中"),
    REGEX_EXACT("REGEX_EXACT", "正则精确匹配"),
    REGEX_CASE_INSENSITIVE("REGEX_CASE_INSENSITIVE", "正则忽略大小写匹配");

    private final String code;
    private final String description;

    public static FilterOperatorEnum of(String code) {
        for (FilterOperatorEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) {
                return e;
            }
        }
        return CONTAINS;
    }
}
