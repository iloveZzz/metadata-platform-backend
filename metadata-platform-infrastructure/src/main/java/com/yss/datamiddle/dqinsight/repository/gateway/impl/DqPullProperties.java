package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 通道拉取配置（dq.pull.base-url）。
 *
 * <p>契约未定义拉取 URL（切片 04 人工审查点）；MVP 假设拉取地址 = {baseUrl}/pull/{channelId}，
 * 返回原始结果内容（GE / 通用 CSV / 通用 API），部署联调时定稿。未配置时拉取按 network 失败。 </p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "dq.pull")
public class DqPullProperties {

    /** 外部 DQ 工具拉取基础地址（默认空 = 未配置，拉取按 network 分类失败） */
    private String baseUrl = "";
}
