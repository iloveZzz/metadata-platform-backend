package com.yss.datasecurity.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 密钥大类枚举
 */
@Getter
@RequiredArgsConstructor
public enum KeyTypeEnum {
    HASH("HASH", "哈希盐值"),
    HASH_SALT("HASH_SALT", "加盐哈希"),
    ENCRYPTION("ENCRYPTION", "加密密钥"),
    SYMMETRIC("SYMMETRIC", "对称密钥"),
    ASYMMETRIC("ASYMMETRIC", "非对称密钥");

    private final String code;
    private final String description;

    public static KeyTypeEnum of(String code) {
        for (KeyTypeEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) {
                return e;
            }
        }
        return HASH;
    }
}
