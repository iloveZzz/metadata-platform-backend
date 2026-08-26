package com.yss.datamiddle.aicontextlayer.mcpserver.transport;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 传输安全测试（SEC-11，契约第 11 节）：
 * Streamable HTTP 仅经 TLS（明文拒绝）；凭据不随查询参数传递（查询参数出现凭据键即拒绝）。
 */
class TransportSecurityGuardTest {

    @Test
    void rejectsPlainHttpForStreamableHttpTransport() {
        assertThatThrownBy(() ->
            TransportSecurityGuard.assertTlsOnly("http://metadata-platform.example/api/assets",
                McpTransportType.STREAMABLE_HTTP))
            .isInstanceOf(McpTransportException.class)
            .hasMessageContaining("TLS");
    }

    @Test
    void acceptsHttpsForStreamableHttpTransport() {
        TransportSecurityGuard.assertTlsOnly("https://metadata-platform.example/api/assets",
            McpTransportType.STREAMABLE_HTTP);
    }

    @Test
    void stdioTransportSkipsUrlCheck() {
        // stdio 无 URL 面，跳过 TLS 检查
        TransportSecurityGuard.assertTlsOnly(null, McpTransportType.STDIO);
    }

    @Test
    void rejectsCredentialKeyInQueryParams() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("token", "abc");
        assertThatThrownBy(() -> TransportSecurityGuard.rejectCredentialInQueryParams(queryParams))
            .isInstanceOf(McpTransportException.class)
            .hasMessageContaining("Authorization");
    }

    @Test
    void rejectsApiKeySecretAndPasswordQueryParams() {
        assertThatThrownBy(() -> TransportSecurityGuard.rejectCredentialInQueryParams(
            Collections.singletonMap("api_key", "abc"))).isInstanceOf(McpTransportException.class);
        assertThatThrownBy(() -> TransportSecurityGuard.rejectCredentialInQueryParams(
            Collections.singletonMap("secret", "abc"))).isInstanceOf(McpTransportException.class);
        assertThatThrownBy(() -> TransportSecurityGuard.rejectCredentialInQueryParams(
            Collections.singletonMap("password", "abc"))).isInstanceOf(McpTransportException.class);
        assertThatThrownBy(() -> TransportSecurityGuard.rejectCredentialInQueryParams(
            Collections.singletonMap("API-Key", "abc"))).isInstanceOf(McpTransportException.class);
    }

    @Test
    void allowsBenignQueryParams() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("keyword", "customer");
        queryParams.put("page", "1");
        queryParams.put("sort", "updatedAt");
        TransportSecurityGuard.rejectCredentialInQueryParams(queryParams);
    }

    @Test
    void allowsNullQueryParams() {
        TransportSecurityGuard.rejectCredentialInQueryParams(null);
    }
}
