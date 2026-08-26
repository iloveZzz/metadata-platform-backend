package com.yss.datamiddle.aicontextlayer.infrastructure.mcpserver;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.CredentialCipher;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 凭据密文引用的本地密钥占位实现 —— 【D3 人工评审点】。
 *
 * <p><b>决策（D3）</b>：MVP 以本地 AES-256-GCM 密钥占位实现 {@link CredentialCipher}
 * 端口（密钥来自配置 {@code acl.security.credential.local-cipher-key}，Base64 编码 32 字节，
 * 缺失 / 非法时启动即失败——fail closed）。凭据明文经加密后才写入
 * {@code agent_credential.credential_ref}，满足 SEC-05「密文存储、不存明文」。</p>
 *
 * <p><b>生产替换</b>：本实现不是生产 KMS。D3 评审确认后应替换为平台密钥管理 /
 * 等价 KMS client（如 AWS KMS Encrypt/Decrypt 或主平台密钥服务，IC-04 归属确认），
 * 经同一 {@link CredentialCipher} 端口接入，领域层与凭据校验实现不变。</p>
 *
 * <p>密文引用格式：{@code local:v1:<base64(iv‖ciphertext)>}（iv 12 字节 GCM nonce），
 * 仅承载密文不承载明文；本类不输出任何含密钥 / 明文的日志（SEC-05/11）。</p>
 */
public class LocalCredentialCipher implements CredentialCipher {

    /** 本地占位实现的密文引用前缀（格式标识；真实 KMS 引用格式由生产实现定义）。 */
    public static final String REFERENCE_PREFIX = "local:v1:";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int KEY_LENGTH_BYTES = 32;

    private final SecretKey key;
    private final SecureRandom secureRandom = new SecureRandom();

    public LocalCredentialCipher(String base64Key) {
        if (base64Key == null || base64Key.trim().isEmpty()) {
            throw new IllegalStateException(
                "ACL 凭据本地密文密钥未配置（acl.security.credential.local-cipher-key），"
                    + "D3 人工评审点：MVP 为本地密钥占位，生产应接入真实 KMS client seam");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("ACL 凭据本地密文密钥必须为 Base64 编码的 32 字节（AES-256）", e);
        }
        if (keyBytes.length != KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                "ACL 凭据本地密文密钥必须为 32 字节（AES-256），实际 " + keyBytes.length + " 字节");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String reference(String plaintextSecret) {
        if (plaintextSecret == null || plaintextSecret.isEmpty()) {
            throw new IllegalArgumentException("凭据明文不能为空");
        }
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintextSecret.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(GCM_IV_LENGTH + ciphertext.length);
            buffer.put(iv).put(ciphertext);
            return REFERENCE_PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("凭据密文引用生成失败", e);
        }
    }

    @Override
    public String dereference(String credentialRef) {
        if (credentialRef == null || !credentialRef.startsWith(REFERENCE_PREFIX)) {
            throw new IllegalArgumentException("不支持的凭据密文引用格式（非 local:v1: 前缀）");
        }
        byte[] data;
        try {
            data = Base64.getDecoder().decode(credentialRef.substring(REFERENCE_PREFIX.length()));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("凭据密文引用解码失败", e);
        }
        if (data.length <= GCM_IV_LENGTH) {
            throw new IllegalStateException("凭据密文引用数据不完整");
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key,
                new GCMParameterSpec(GCM_TAG_LENGTH_BITS, data, 0, GCM_IV_LENGTH));
            byte[] plaintext = cipher.doFinal(data, GCM_IV_LENGTH, data.length - GCM_IV_LENGTH);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("凭据密文引用解引用失败", e);
        }
    }
}
