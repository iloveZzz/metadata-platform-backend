package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 通道凭证加密测试（DQI-SLICE-04-WU2，C15 / C19）。
 *
 * <p>成熟加密库 JDK JCE AES/GCM：加解密往返一致；随机 IV（同明文两次加密密文不同）；
 * 密文不回传（仅 authConfigured）；密钥长度校验。</p>
 */
class ChannelCryptoTest {

    private static ChannelTokenCipher cipher() {
        ChannelTokenCipherProperties properties = new ChannelTokenCipherProperties();
        return new ChannelTokenCipher(properties);
    }

    @Test
    void encryptDecryptRoundTrips() {
        ChannelTokenCipher cipher = cipher();

        String ciphertext = cipher.encrypt("channel-token-secret");
        String plaintext = cipher.decrypt(ciphertext);

        assertThat(plaintext).isEqualTo("channel-token-secret");
        assertThat(ciphertext).isNotEqualTo("channel-token-secret"); // 密文形态
    }

    @Test
    void randomIvProducesDifferentCiphertextForSamePlaintext() {
        ChannelTokenCipher cipher = cipher();

        String first = cipher.encrypt("same-token");
        String second = cipher.encrypt("same-token");

        assertThat(first).isNotEqualTo(second);
        assertThat(cipher.decrypt(first)).isEqualTo("same-token");
        assertThat(cipher.decrypt(second)).isEqualTo("same-token");
    }

    @Test
    void tamperedCiphertextFailsDecryption() {
        ChannelTokenCipher cipher = cipher();
        String ciphertext = cipher.encrypt("token");
        String tampered = ciphertext.substring(0, ciphertext.length() - 2) + "AA";

        assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void invalidKeyLengthRejected() {
        ChannelTokenCipherProperties properties = new ChannelTokenCipherProperties();
        properties.setSecretKey("MTIzNA=="); // 4 字节，非法

        assertThatThrownBy(() -> new ChannelTokenCipher(properties))
                .isInstanceOf(IllegalStateException.class);
    }
}
