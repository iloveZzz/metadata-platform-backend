package com.yss.datasecurity.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 脱敏算法类型枚举
 */
@Getter
@RequiredArgsConstructor
public enum MaskingAlgorithmTypeEnum {
    MASKING("MASKING", "掩码脱敏"),
    HASH_SALT("HASH_SALT", "加盐哈希"),
    ENCRYPTION("ENCRYPTION", "加密脱敏"),
    SPECIAL("SPECIAL", "特殊清洗");

    private final String code;
    private final String description;

    public static MaskingAlgorithmTypeEnum of(String code) {
        for (MaskingAlgorithmTypeEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) {
                return e;
            }
        }
        return MASKING;
    }
}
