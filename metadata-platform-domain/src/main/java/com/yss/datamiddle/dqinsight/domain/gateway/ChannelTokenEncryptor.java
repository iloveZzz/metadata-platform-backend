package com.yss.datamiddle.dqinsight.domain.gateway;

/**
 * 通道凭证加密端口（成熟加密库，C15；密文不回传，仅 authConfigured，C19）。
 *
 * <p>Infrastructure 以 JDK JCE AES/GCM 实现（随机 IV，密文 = Base64(IV ‖ 密文)）；Application 只依赖本端口，
 * 不接触加密实现细节。</p>
 */
public interface ChannelTokenEncryptor {

    /**
     * 加密（随机 IV；返回 Base64(IV ‖ 密文)）。
     */
    String encrypt(String plaintext);

    /**
     * 解密（密文损坏 / 密钥轮换抛 IllegalStateException，调用方按 fail-closed 处理）。
     */
    String decrypt(String ciphertext);
}
