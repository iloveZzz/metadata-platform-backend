package com.yss.datamiddle.dqinsight.domain.gateway;

import com.yss.datamiddle.dqinsight.domain.model.ChannelCredential;

import java.util.Optional;

/**
 * 通道凭证存储端口（每通道独立 AK/SK，SB-09 基线）。
 *
 * <p>凭证加密存储（dq_channel.auth_token_enc）由切片 04 落地；本切片（WU4）以端口 + 测试 fixture
 * 验证认证与脱敏逻辑（合同 seam_deferred）。</p>
 */
public interface ChannelCredentialStore {

    /**
     * 按通道 Token 的 SHA-256 指纹查找对应通道凭证。
     *
     * @param tokenFingerprint 通道 Token 的 SHA-256 十六进制指纹
     * @return 命中的通道凭证（查无 → 认证失败 err.dq.auth.invalid）
     */
    Optional<ChannelCredential> findByTokenFingerprint(String tokenFingerprint);
}
