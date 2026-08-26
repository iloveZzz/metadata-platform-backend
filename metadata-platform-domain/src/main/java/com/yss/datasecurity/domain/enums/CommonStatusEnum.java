package com.yss.datasecurity.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 业务通用生效/启停状态枚举
 */
@Getter
@RequiredArgsConstructor
public enum CommonStatusEnum {
    ENABLED("ENABLED", "已启用"),
    DISABLED("DISABLED", "已停用");

    private final String code;
    private final String description;

    public static boolean isEnabled(String code) {
        return ENABLED.code.equalsIgnoreCase(code);
    }

    public static boolean isDisabled(String code) {
        return DISABLED.code.equalsIgnoreCase(code);
    }

    public static CommonStatusEnum of(String code) {
        for (CommonStatusEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) {
                return e;
            }
        }
        return DISABLED;
    }
}
