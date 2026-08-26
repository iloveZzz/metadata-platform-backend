package com.yss.datamiddle.aicontextlayer.infrastructure.mcpserver;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 本地密钥占位密文实现单测（WU-01-03 / D3 评审点）：
 * 凭据明文 ↔ KMS 密文引用往返一致；密文引用不含明文（SEC-05）；缺失 / 非法密钥 fail-fast。
 */
class LocalCredentialCipherTest {

    /** 测试密钥：Base64 编码 32 字节（AES-256）。 */
    private static final String TEST_KEY =
        Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    private final LocalCredentialCipher cipher = new LocalCredentialCipher(TEST_KEY);

    @Test
    void referenceRoundTripReturnsOriginalPlaintext() {
        String secret = "test-agent-secret-value";

        String ref = cipher.reference(secret);

        assertThat(ref).startsWith(LocalCredentialCipher.REFERENCE_PREFIX);
        assertThat(cipher.dereference(ref)).isEqualTo(secret);
    }

    @Test
    void referenceDoesNotContainPlaintext() {
        // SEC-05 / E6 落库检查：密文引用不承载明文
        String secret = "super-secret-token-123";
        String ref = cipher.reference(secret);

        assertThat(ref).isNotEqualTo(secret);
        assertThat(ref).doesNotContain(secret);
    }

    @Test
    void referencesAreRandomizedWithRandomIv() {
        String secret = "same-secret";
        String ref1 = cipher.reference(secret);
        String ref2 = cipher.reference(secret);

        assertThat(ref1).isNotEqualTo(ref2);
        assertThat(cipher.dereference(ref1)).isEqualTo(cipher.dereference(ref2));
    }

    @Test
    void rejectsNullAndEmptyPlaintext() {
        assertThatIllegalArgumentException().isThrownBy(() -> cipher.reference(null));
        assertThatIllegalArgumentException().isThrownBy(() -> cipher.reference(""));
    }

    @Test
    void rejectsMissingKeyAtConstruction() {
        assertThatThrownBy(() -> new LocalCredentialCipher(""))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("local-cipher-key");
        assertThatThrownBy(() -> new LocalCredentialCipher(null))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsInvalidKeyLengthAtConstruction() {
        String shortKey = Base64.getEncoder().encodeToString("too-short".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> new LocalCredentialCipher(shortKey))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("32");
    }

    @Test
    void rejectsUnsupportedReferenceFormat() {
        assertThatThrownBy(() -> cipher.dereference("kms:alias/unknown/ciphertext"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("local:v1:");
    }
}
