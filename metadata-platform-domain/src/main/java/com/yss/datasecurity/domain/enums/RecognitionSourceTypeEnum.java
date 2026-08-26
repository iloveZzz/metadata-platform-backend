package com.yss.datasecurity.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 敏感识别打标来源类型枚举
 */
@Getter
@RequiredArgsConstructor
public enum RecognitionSourceTypeEnum {
    RULE_AUTO("RULE_AUTO", "规则自动识别"),
    MANUAL_LOCKED("MANUAL_LOCKED", "人工校准锁定"),
    LINEAGE_INHERITED("LINEAGE_INHERITED", "血缘自动继承");

    private final String code;
    private final String description;

    public static RecognitionSourceTypeEnum of(String code) {
        for (RecognitionSourceTypeEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) {
                return e;
            }
        }
        return RULE_AUTO;
    }
}
