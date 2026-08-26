package com.yss.datasecurity.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 加密与哈希算法枚举
 */
@Getter
@RequiredArgsConstructor
public enum CryptoAlgorithmEnum {
    AES("AES", "高级加密标准(对称)"),
    DES("DES", "数据加密标准(对称)"),
    DES3("3DES", "三重数据加密标准(对称)"),
    SM2("SM2", "国密非对称加密算法"),
    SM4("SM4", "国密对称分组密码算法"),
    RSA("RSA", "RSA公钥加密算法"),
    FF1("FF1", "格式保留加密(FPE-FF1)"),
    FPE("FPE", "格式保留加密"),
    SHA256("SHA-256", "SHA-256安全散列算法"),
    MD5("MD5", "MD5散列算法");

    private final String code;
    private final String description;

    public static CryptoAlgorithmEnum of(String code) {
        if (code == null) return AES;
        for (CryptoAlgorithmEnum e : values()) {
            if (e.code.equalsIgnoreCase(code) || e.name().equalsIgnoreCase(code)) {
                return e;
            }
        }
        return AES;
    }
}
