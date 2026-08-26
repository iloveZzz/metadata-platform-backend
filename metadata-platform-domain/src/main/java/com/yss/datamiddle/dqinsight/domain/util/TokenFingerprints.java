package com.yss.datamiddle.dqinsight.domain.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 通道 Token 指纹工具（SHA-256 十六进制，小写）。
 *
 * <p>认证中间件 / 凭证存储只以指纹查询与比对，不落明文凭证日志（C19 脱敏）。
 * Web 模块既有 TokenFingerprint 为切片 01 实现，本工具供 Domain / Infrastructure 侧复用。</p>
 */
public final class TokenFingerprints {

    private TokenFingerprints() {
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
