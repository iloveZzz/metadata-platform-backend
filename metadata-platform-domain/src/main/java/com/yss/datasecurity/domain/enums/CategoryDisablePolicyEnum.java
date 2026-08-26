package com.yss.datasecurity.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 数据分类停用级联策略枚举
 */
@Getter
@RequiredArgsConstructor
public enum CategoryDisablePolicyEnum {
    RETAIN_TAGS("RETAIN_TAGS", "保留已打标字段"),
    DELETE_TAGS("DELETE_TAGS", "删除已打标字段");

    private final String code;
    private final String description;

    public static CategoryDisablePolicyEnum of(String code) {
        for (CategoryDisablePolicyEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) {
                return e;
            }
        }
        return RETAIN_TAGS;
    }
}
