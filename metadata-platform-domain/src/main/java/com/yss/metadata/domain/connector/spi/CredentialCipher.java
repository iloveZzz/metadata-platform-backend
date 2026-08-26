package com.yss.metadata.domain.connector.spi;

/**
 * 凭据加密引用端口（Domain 定义，Infrastructure 实现）。
 *
 * <p>密码明文仅在此端口加密后以引用形式持久化，不落库明文；
 * 生产级加密方案（KMS/信封加密）为合同 human review 项。</p>
 */
public interface CredentialCipher {

    /**
     * 加密明文，返回凭据引用（加密引用可能含不可逆变换或密文）。
     */
    String encrypt(String plaintext);

    /**
     * 依据凭据引用解密还原明文（供连接测试等需要真实凭据的场景使用）。
     */
    String decrypt(String ref);
}
