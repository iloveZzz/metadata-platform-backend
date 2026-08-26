package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.datamiddle.dqinsight.domain.gateway.ChannelCredentialStore;
import com.yss.datamiddle.dqinsight.domain.model.ChannelCredential;
import com.yss.datamiddle.dqinsight.domain.util.TokenFingerprints;
import com.yss.datamiddle.dqinsight.repository.DqChannelRepository;
import com.yss.datamiddle.dqinsight.repository.entity.DqChannelPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 通道凭证存储实现（dq_channel.auth_token_enc 解密后按指纹比对，切片 04 闭环切片 01 认证 seam）。
 *
 * <p>解密失败（密钥轮换 / 密文损坏）按未命中处理（fail-closed，不泄露凭证，C19）。
 * MVP 通道量级小，全量解密比对可接受；指纹索引列为 P1（数据架构 §5 无该列，不触发 schema 变更）。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DqChannelCredentialStoreImpl implements ChannelCredentialStore {

    private final DqChannelRepository dqChannelRepository;
    private final ChannelTokenCipher cipher;

    @Override
    public Optional<ChannelCredential> findByTokenFingerprint(String tokenFingerprint) {
        List<DqChannelPO> channels = dqChannelRepository.selectList(Wrappers.<DqChannelPO>lambdaQuery()
                .isNull(DqChannelPO::getDeletedAt)
                .isNotNull(DqChannelPO::getAuthTokenEnc));
        for (DqChannelPO po : channels) {
            String token = decryptSafely(po.getAuthTokenEnc());
            if (token != null && tokenFingerprint.equals(TokenFingerprints.sha256Hex(token))) {
                return Optional.of(new ChannelCredential(String.valueOf(po.getId()),
                        po.getName() == null ? "" : po.getName(),
                        Boolean.TRUE.equals(po.getAuthConfigured())));
            }
        }
        return Optional.empty();
    }

    private String decryptSafely(String ciphertext) {
        try {
            return cipher.decrypt(ciphertext);
        } catch (IllegalStateException e) {
            log.warn("通道凭证解密失败（按未命中处理，fail-closed）：通道密钥可能已轮换");
            return null;
        }
    }
}
