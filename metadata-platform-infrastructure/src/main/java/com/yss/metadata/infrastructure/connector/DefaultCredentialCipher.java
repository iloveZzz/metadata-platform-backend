package com.yss.metadata.infrastructure.connector;

import com.yss.metadata.domain.connector.spi.CredentialCipher;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 凭据加密引用默认实现（seam，合同 human review 项）。
 *
 * <p>当前实现仅保证"密码不落库明文"（Base64 可逆变换 + 前缀标识）；
 * 生产级加密方案（KMS/信封加密）需人工审查确认后再替换，不得直接用于生产。</p>
 */
@Component
public class DefaultCredentialCipher implements CredentialCipher {

    private static final String REF_PREFIX = "seam-base64:";

    @Override
    public String encrypt(String plaintext) {
        return REF_PREFIX + Base64.getEncoder().encodeToString(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String decrypt(String ref) {
        String encoded = ref.substring(REF_PREFIX.length());
        return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
}
