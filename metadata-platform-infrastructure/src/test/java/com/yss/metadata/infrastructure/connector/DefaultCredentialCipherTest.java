package com.yss.metadata.infrastructure.connector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 凭据加密引用实现行为测试（WU-01-01）。
 *
 * <p>验证：加密结果不等于明文且不包含明文；解密可还原（供连接测试 SPI 使用）。
 * 本实现为 seam（合同 human review 项），生产级加密方案待人工确认。</p>
 */
class DefaultCredentialCipherTest {

    private final DefaultCredentialCipher cipher = new DefaultCredentialCipher();

    @Test
    @DisplayName("加密结果不是明文且不包含明文")
    void encryptIsNotPlaintext() {
        String ref = cipher.encrypt("pwd-plaintext-123");

        assertThat(ref).isNotEqualTo("pwd-plaintext-123");
        assertThat(ref).doesNotContain("pwd-plaintext-123");
    }

    @Test
    @DisplayName("解密可还原明文（供连接测试 SPI 解析凭据）")
    void decryptRoundTrips() {
        String ref = cipher.encrypt("pwd-plaintext-123");

        assertThat(cipher.decrypt(ref)).isEqualTo("pwd-plaintext-123");
    }
}
