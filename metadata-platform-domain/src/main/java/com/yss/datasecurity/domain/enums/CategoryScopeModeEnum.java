package com.yss.datasecurity.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 识别规则圈选数据分类模式枚举
 */
@Getter
@RequiredArgsConstructor
public enum CategoryScopeModeEnum {
    ALL("ALL", "全部分类"),
    TREE_NODE("TREE_NODE", "指定目录下所有分类"),
    SPECIFIC("SPECIFIC", "指定数据分类");

    private final String code;
    private final String description;

    public static CategoryScopeModeEnum of(String code) {
        for (CategoryScopeModeEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) {
                return e;
            }
        }
        return ALL;
    }
}
