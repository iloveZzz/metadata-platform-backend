package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 通道凭证加密配置（dq.channel-crypto.secret-key）。
 *
 * <p>生产必须配置独立密钥；默认值为开发 / 测试占位（16 字节，Base64），使用默认值启动时记录警告。
 * 凭证加密为 C15 人工安全审查点（成熟加密库，不自研；本实现使用 JDK JCE AES/GCM）。</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "dq.channel-crypto")
public class ChannelTokenCipherProperties {

    /**
     * AES 密钥（Base64 编码，16 / 24 / 32 字节）。默认 = "0123456789abcdef" 的 Base64（开发占位）。
     */
    private String secretKey = "MDEyMzQ1Njc4OWFiY2RlZg==";
}
