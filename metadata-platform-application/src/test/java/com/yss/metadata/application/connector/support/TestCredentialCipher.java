package com.yss.metadata.application.connector.support;

import com.yss.metadata.domain.connector.spi.CredentialCipher;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 凭据加密引用测试实现（seam-deferred）。
 *
 * <p>仅用于验证"密码不落库明文"的行为契约（加密结果 ≠ 明文，且不包含明文）；
 * 生产级加密方案（KMS/信封加密）为合同 human review 项。</p>
 */
public class TestCredentialCipher implements CredentialCipher {

    @Override
    public String encrypt(String plaintext) {
        return "seam-base64:" + Base64.getEncoder().encodeToString(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String decrypt(String ref) {
        String encoded = ref.substring("seam-base64:".length());
        return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
}
