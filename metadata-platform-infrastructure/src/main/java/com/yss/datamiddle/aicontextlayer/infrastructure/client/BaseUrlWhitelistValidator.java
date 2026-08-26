package com.yss.datamiddle.aicontextlayer.infrastructure.client;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 外部服务 Base URL 白名单校验器（SEC-11 SSRF 防御）。
 *
 * <p>限制下游请求的目标 Base URL 只能指向受信任的主平台或内网服务，
 * 拒绝任意 URL 反射或云厂商元数据端点访问（如 169.254.169.254）。</p>
 */
public class BaseUrlWhitelistValidator {

    private final Set<String> allowedHosts;

    public BaseUrlWhitelistValidator() {
        Set<String> defaults = new HashSet<>();
        defaults.add("localhost");
        defaults.add("127.0.0.1");
        defaults.add("metadata-platform");
        defaults.add("192.168.167.26");
        this.allowedHosts = Collections.unmodifiableSet(defaults);
    }

    public BaseUrlWhitelistValidator(Set<String> allowedHosts) {
        if (allowedHosts == null || allowedHosts.isEmpty()) {
            this.allowedHosts = Collections.emptySet();
        } else {
            this.allowedHosts = Collections.unmodifiableSet(new HashSet<>(allowedHosts));
        }
    }

    /**
     * 校验 URL 是否在 Base URL 白名单范围内。
     *
     * @param urlString 待校验 URL
     * @throws McpException 如果 URL 不合法或不在白名单内
     */
    public void validate(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            throw McpException.of(McpErrorCode.INVALID_PARAMS);
        }
        try {
            URI uri = URI.create(urlString.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw McpException.of(McpErrorCode.INVALID_PARAMS);
            }
            String host = uri.getHost();
            if (host == null || host.trim().isEmpty()) {
                throw McpException.of(McpErrorCode.INVALID_PARAMS);
            }
            if (isCloudMetadataHost(host)) {
                throw McpException.of(McpErrorCode.UNAUTHORIZED);
            }
            if (!allowedHosts.contains(host.toLowerCase())) {
                throw McpException.of(McpErrorCode.UNAUTHORIZED);
            }
        } catch (IllegalArgumentException e) {
            throw McpException.of(McpErrorCode.INVALID_PARAMS);
        }
    }

    private boolean isCloudMetadataHost(String host) {
        return "169.254.169.254".equals(host) || "metadata.google.internal".equalsIgnoreCase(host);
    }
}
