package com.yss.datasecurity.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 识别结果记录状态枚举
 */
@Getter
@RequiredArgsConstructor
public enum RecognitionStatusEnum {
    CONFIRMED("CONFIRMED", "已确认生效"),
    UNCONFIRMED("UNCONFIRMED", "待确认候选"),
    ACTIVE("ACTIVE", "生效中"),
    EXPIRED("EXPIRED", "已失效");

    private final String code;
    private final String description;

    public static RecognitionStatusEnum of(String code) {
        for (RecognitionStatusEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) {
                return e;
            }
        }
        return UNCONFIRMED;
    }
}
