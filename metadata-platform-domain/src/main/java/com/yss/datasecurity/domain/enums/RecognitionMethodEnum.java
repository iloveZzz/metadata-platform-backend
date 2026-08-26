package com.yss.datasecurity.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 敏感数据识别方式枚举
 */
@Getter
@RequiredArgsConstructor
public enum RecognitionMethodEnum {
    AUTO("AUTO", "自动识别"),
    MANUAL("MANUAL", "手动指定"),
    LINEAGE("LINEAGE", "基于血缘自动继承");

    private final String code;
    private final String description;

    public static boolean isManual(String code) {
        return MANUAL.code.equalsIgnoreCase(code);
    }

    public static boolean isAuto(String code) {
        return AUTO.code.equalsIgnoreCase(code);
    }

    public static RecognitionMethodEnum of(String code) {
        for (RecognitionMethodEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) {
                return e;
            }
        }
        return AUTO;
    }
}
