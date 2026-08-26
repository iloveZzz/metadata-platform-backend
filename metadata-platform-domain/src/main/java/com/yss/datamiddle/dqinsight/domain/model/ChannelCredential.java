package com.yss.datamiddle.dqinsight.domain.model;

import lombok.Getter;

import java.io.Serializable;

/**
 * 通道凭证（每通道独立 AK/SK，SB-09 基线）。
 *
 * <p>凭证存储（dq_channel 表 / 解密实现）由切片 04 落地；本切片以 ChannelCredentialStore
 * 端口 + 测试 fixture 验证认证与脱敏逻辑（合同 seam_deferred）。密文不回传，仅返回元信息。</p>
 */
@Getter
public class ChannelCredential implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 通道 ID */
    private final String channelId;

    /** 通道 AK（元信息，不回传凭证） */
    private final String accessKey;

    /** 认证是否已配置 */
    private final boolean authConfigured;

    public ChannelCredential(String channelId, String accessKey, boolean authConfigured) {
        this.channelId = channelId;
        this.accessKey = accessKey;
        this.authConfigured = authConfigured;
    }
}
