package com.yss.datamiddle.aicontextlayer.mcpserver.transport;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 凭据提取（SEC-11，契约第 11 节）：凭据仅经 Authorization header 传递。
 *
 * <p>仅支持 {@code Authorization: Bearer <token>} scheme；header 名大小写不敏感；
 * 缺失 / 空白 / 非 Bearer scheme 一律视为未携带凭据（由鉴权路径统一拒绝为
 * {@code unauthorized}，禁匿名 SEC-05）。查询参数中的凭据由
 * {@link TransportSecurityGuard#rejectCredentialInQueryParams(Map)} 拒绝。</p>
 */
public final class AuthorizationHeaderExtractor {

    private static final String HEADER_AUTHORIZATION = "authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();

    private AuthorizationHeaderExtractor() {
    }

    /**
     * 从请求头提取 Bearer Token。
     *
     * @param headers 请求头（键大小写不敏感）
     * @return Bearer Token；缺失 / 空白 / 非 Bearer scheme 返回 {@link Optional#empty()}
     */
    public static Optional<String> extract(Map<String, String> headers) {
        if (headers == null) {
            return Optional.empty();
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null
                && HEADER_AUTHORIZATION.equals(entry.getKey().toLowerCase(Locale.ROOT))) {
                return extractBearerToken(entry.getValue());
            }
        }
        return Optional.empty();
    }

    private static Optional<String> extractBearerToken(String authorizationValue) {
        if (authorizationValue == null || authorizationValue.trim().isEmpty()) {
            return Optional.empty();
        }
        String value = authorizationValue.trim();
        if (value.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX_LENGTH)) {
            String token = value.substring(BEARER_PREFIX_LENGTH).trim();
            return token.isEmpty() ? Optional.empty() : Optional.of(token);
        }
        // 非 Bearer scheme 不支持 → 视为凭据缺失（unauthorized）
        return Optional.empty();
    }
}
