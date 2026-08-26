package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.yss.datamiddle.dqinsight.InfraTestApplication;
import com.yss.datamiddle.dqinsight.domain.gateway.ChannelCredentialStore;
import com.yss.datamiddle.dqinsight.domain.gateway.ChannelTokenEncryptor;
import com.yss.datamiddle.dqinsight.domain.model.ChannelCredential;
import com.yss.datamiddle.dqinsight.domain.model.ChannelType;
import com.yss.datamiddle.dqinsight.domain.model.FormatType;
import com.yss.datamiddle.dqinsight.domain.model.IngestionChannel;
import com.yss.datamiddle.dqinsight.domain.util.TokenFingerprints;
import com.yss.datamiddle.dqinsight.repository.DqChannelRepository;
import com.yss.datamiddle.dqinsight.infrastructure.convertor.DqChannelConvertor;
import com.yss.datamiddle.dqinsight.repository.entity.DqChannelPO;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 通道凭证存储端到端测试（DQI-SLICE-04-WU2，闭环切片 01 认证 seam，C19）。
 *
 * <p>加密入库 → 按指纹查询命中（切片 01 ChannelAuthFilter 以指纹查询）；错误 Token 未命中；
 * 密文损坏 fail-closed（按未命中处理，不泄露凭证）。</p>
 */
@SpringBootTest(classes = InfraTestApplication.class)
@Transactional
class ChannelCredentialStoreTest {

    private static final String TOKEN = "channel-token-cred-004";

    private final DqChannelConvertor dqChannelConvertor = Mappers.getMapper(DqChannelConvertor.class);

    @Autowired
    private ChannelCredentialStore channelCredentialStore;

    @Autowired
    private ChannelTokenEncryptor channelTokenEncryptor;

    @Autowired
    private DqChannelRepository dqChannelRepository;

    @Test
    void findByTokenFingerprintMatchesEncryptedChannelToken() {
        Long channelId = saveChannelWithToken(TOKEN);

        Optional<ChannelCredential> found = channelCredentialStore
                .findByTokenFingerprint(TokenFingerprints.sha256Hex(TOKEN));

        assertThat(found).isPresent();
        assertThat(found.get().getChannelId()).isEqualTo(String.valueOf(channelId));
        assertThat(found.get().isAuthConfigured()).isTrue();
    }

    @Test
    void wrongTokenDoesNotMatch() {
        saveChannelWithToken(TOKEN);

        Optional<ChannelCredential> found = channelCredentialStore
                .findByTokenFingerprint(TokenFingerprints.sha256Hex("wrong-token"));

        assertThat(found).isEmpty();
    }

    @Test
    void channelWithoutTokenDoesNotAuthenticate() {
        IngestionChannel channel = IngestionChannel.create("无凭证通道", ChannelType.API_PUSH, null,
                FormatType.GE, null, true);
        DqChannelPO po = dqChannelConvertor.toPO(channel);
        dqChannelRepository.insert(po);

        Optional<ChannelCredential> found = channelCredentialStore
                .findByTokenFingerprint(TokenFingerprints.sha256Hex(TOKEN));

        assertThat(found).isEmpty();
    }

    @Test
    void corruptCiphertextFailsClosedWithoutCredentialLeak() {
        Long channelId = saveChannelWithToken(TOKEN);
        DqChannelPO po = dqChannelRepository.selectById(channelId);
        po.setAuthTokenEnc("corrupt-not-valid-base64!!!");
        dqChannelRepository.updateById(po);

        Optional<ChannelCredential> found = channelCredentialStore
                .findByTokenFingerprint(TokenFingerprints.sha256Hex(TOKEN));

        assertThat(found).isEmpty(); // fail-closed
    }

    private Long saveChannelWithToken(String token) {
        IngestionChannel channel = IngestionChannel.create("凭证通道", ChannelType.API_PUSH, null,
                FormatType.GE, null, true);
        channel.setAuthTokenEncrypted(channelTokenEncryptor.encrypt(token));
        DqChannelPO po = dqChannelConvertor.toPO(channel);
        dqChannelRepository.insert(po);
        return po.getId();
    }
}
