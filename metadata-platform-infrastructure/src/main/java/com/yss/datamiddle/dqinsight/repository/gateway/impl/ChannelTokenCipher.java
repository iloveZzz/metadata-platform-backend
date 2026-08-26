package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.yss.datamiddle.dqinsight.domain.gateway.ChannelTokenEncryptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 通道凭证加密（成熟加密库 JDK JCE：AES/GCM/NoPadding，随机 IV，C15 / C19）。
 *
 * <p>密文形态 = Base64(IV(12B) ‖ GCM 密文(128-bit tag)）；每次加密随机 IV → 同明文密文不同；
 * 仅存储密文，API 不回传（authConfigured 布尔）。密钥来自 dq.channel-crypto.secret-key
 * （生产必须配置，默认开发占位启动时警告）。</p>
 */
@Slf4j
@Component
public class ChannelTokenCipher implements ChannelTokenEncryptor {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private static final int IV_LENGTH = 12;

    private static final int TAG_BITS = 128;

    /** 开发默认密钥标记（生产必须覆盖 dq.channel-crypto.secret-key） */
    private static final String DEV_DEFAULT_KEY = "MDEyMzQ1Njc4OWFiY2RlZg==";

    private final SecretKeySpec key;

    public ChannelTokenCipher(ChannelTokenCipherProperties properties) {
        String secretKey = properties == null ? null : properties.getSecretKey();
        if (DEV_DEFAULT_KEY.equals(secretKey)) {
            log.warn("通道凭证使用开发默认密钥（dq.channel-crypto.secret-key 未配置），生产环境必须配置独立密钥");
        }
        byte[] keyBytes = Base64.getDecoder().decode(secretKey == null ? "" : secretKey);
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalStateException("dq.channel-crypto.secret-key 必须为 16 / 24 / 32 字节（Base64）");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 加密（随机 IV；返回 Base64(IV ‖ 密文)）。
     */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("通道凭证加密失败", e);
        }
    }

    /**
     * 解密（Base64(IV ‖ 密文) → 明文；失败抛 IllegalStateException，调用方按认证失败处理）。
     */
    public String decrypt(String ciphertext) {
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            if (combined.length <= IV_LENGTH) {
                throw new IllegalArgumentException("密文长度非法");
            }
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("通道凭证解密失败", e);
        }
    }
}
