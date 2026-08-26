package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据域 RBAC 与操作权限配置（dq.rbac，切片 05 MVP）。
 *
 * <p>当前用户上下文 starter（yss-userinfo）未入脚手架，MVP 以配置驱动权限来源
 * （OQ-05 人工审查点：真实 RBAC 由主平台接入后以端口实现替换，本配置保留为兜底 / 测试开关）。</p>
 *
 * <ul>
 *   <li>{@code visible-domains}：当前用户可见数据域（空 = 不限制，与切片 03 seam 语义一致）；</li>
 *   <li>{@code deny-capabilities}：被拒绝的操作能力码（见 DqCapabilities；空 = 全部操作允许）。</li>
 * </ul>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "dq.rbac")
public class DqRbacProperties {

    /**
     * 当前用户可见数据域（逗号分隔配置绑定为 List；空 = 不限制）。
     */
    private List<String> visibleDomains = new ArrayList<>();

    /**
     * 被拒绝的操作能力码（空 = 全部操作允许；命中则 403 err.dq.forbidden）。
     */
    private List<String> denyCapabilities = new ArrayList<>();
}
