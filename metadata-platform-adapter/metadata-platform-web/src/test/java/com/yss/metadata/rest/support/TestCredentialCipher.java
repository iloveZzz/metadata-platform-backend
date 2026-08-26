package com.yss.metadata.rest.support;

import com.yss.metadata.domain.connector.spi.CredentialCipher;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 凭据加密引用测试实现（seam-deferred）。
 *
 * <p>仅用于验证"密码不落库明文"的契约行为。</p>
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
