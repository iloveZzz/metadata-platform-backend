package com.yss.datamiddle.aicontextlayer.mcpserver.transport;

import java.net.URI;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 传输安全守卫（SEC-11，契约第 11 节）。
 *
 * <ul>
 *   <li>Streamable HTTP 仅经 TLS（明文拒绝）</li>
 *   <li>凭据仅经 Authorization header 传递：查询参数出现凭据类键（token / api_key /
 *       key / secret / password 等）即拒绝请求</li>
 * </ul>
 */
public final class TransportSecurityGuard {

    private static final String HTTPS_SCHEME = "https";
    private static final String MESSAGE_PLAIN_HTTP = "仅支持 TLS 加密连接（SEC-11）";
    private static final String MESSAGE_CREDENTIAL_IN_QUERY = "凭据仅允许经 Authorization header 传递（SEC-11）";

    /** 凭据类查询参数键（大小写不敏感）。 */
    private static final Pattern CREDENTIAL_QUERY_KEY_PATTERN =
        Pattern.compile("(?i)(token|api[_-]?key|key|secret|password|access[_-]?token|authorization)");

    private TransportSecurityGuard() {
    }

    /**
     * Streamable HTTP 仅经 TLS：非 https scheme 拒绝（明文拒绝）。
     *
     * @param url          请求 URL；stdio transport 下可为 null
     * @param transportType transport 类型
     * @throws McpTransportException 明文 Streamable HTTP 连接
     */
    public static void assertTlsOnly(String url, McpTransportType transportType) {
        if (transportType != McpTransportType.STREAMABLE_HTTP) {
            return;
        }
        URI uri = URI.create(url);
        if (!HTTPS_SCHEME.equalsIgnoreCase(uri.getScheme())) {
            throw new McpTransportException(MESSAGE_PLAIN_HTTP);
        }
    }

    /**
     * 凭据不随查询参数传递：查询参数出现凭据类键即拒绝。
     *
     * @param queryParams 请求查询参数；null 视为无
     * @throws McpTransportException 查询参数包含凭据类键
     */
    public static void rejectCredentialInQueryParams(Map<String, String> queryParams) {
        if (queryParams == null) {
            return;
        }
        for (String key : queryParams.keySet()) {
            if (key != null && CREDENTIAL_QUERY_KEY_PATTERN.matcher(key).matches()) {
                throw new McpTransportException(MESSAGE_CREDENTIAL_IN_QUERY);
            }
        }
    }
}
