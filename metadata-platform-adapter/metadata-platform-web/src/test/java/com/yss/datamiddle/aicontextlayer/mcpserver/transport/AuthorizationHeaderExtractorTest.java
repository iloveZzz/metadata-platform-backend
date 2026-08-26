package com.yss.datamiddle.aicontextlayer.mcpserver.transport;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 凭据仅经 Authorization header 传递测试（SEC-11，契约第 11 节）：
 * header 名大小写不敏感；仅支持 Bearer scheme；缺失 / 空凭据 → 空。
 */
class AuthorizationHeaderExtractorTest {

    @Test
    void extractsBearerTokenFromAuthorizationHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer abc-123");
        assertThat(AuthorizationHeaderExtractor.extract(headers))
            .isEqualTo(Optional.of("abc-123"));
    }

    @Test
    void headerNameIsCaseInsensitive() {
        Map<String, String> headers = new HashMap<>();
        headers.put("authorization", "Bearer abc-123");
        assertThat(AuthorizationHeaderExtractor.extract(headers))
            .isEqualTo(Optional.of("abc-123"));
    }

    @Test
    void returnsEmptyWhenHeaderMissing() {
        Map<String, String> headers = Collections.singletonMap("X-Other", "value");
        assertThat(AuthorizationHeaderExtractor.extract(headers)).isEmpty();
    }

    @Test
    void returnsEmptyForNullHeaders() {
        assertThat(AuthorizationHeaderExtractor.extract(null)).isEmpty();
    }

    @Test
    void returnsEmptyForNonBearerScheme() {
        // 非 Bearer scheme（如 Basic）不支持 → 视为凭据缺失（unauthorized）
        Map<String, String> headers = Collections.singletonMap("Authorization", "Basic abc");
        assertThat(AuthorizationHeaderExtractor.extract(headers)).isEmpty();
    }

    @Test
    void returnsEmptyForEmptyToken() {
        Map<String, String> headers = Collections.singletonMap("Authorization", "Bearer   ");
        assertThat(AuthorizationHeaderExtractor.extract(headers)).isEmpty();
    }
}
