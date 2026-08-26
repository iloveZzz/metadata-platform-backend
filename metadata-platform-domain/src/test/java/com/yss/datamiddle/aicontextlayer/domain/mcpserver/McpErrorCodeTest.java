package com.yss.datamiddle.aicontextlayer.domain.mcpserver;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 9 错误码全集框架测试（冻结契约第 10.1 节，决策 D-05）：
 * 错误码全集与可重试语义必须与契约表逐项一致。
 */
class McpErrorCodeTest {

    private static final List<String> CONTRACT_CODES = Arrays.asList(
        "unauthorized",
        "tool_not_found",
        "invalid_params",
        "asset_not_found",
        "upstream_timeout",
        "upstream_unavailable",
        "upstream_too_large",
        "rate_limited",
        "internal_error"
    );

    @Test
    void allNineErrorCodesPresent() {
        List<String> actualCodes = Arrays.stream(McpErrorCode.values())
            .map(McpErrorCode::getCode)
            .collect(Collectors.toList());
        assertThat(actualCodes).containsExactlyElementsOf(CONTRACT_CODES);
    }

    @Test
    void retryableFlagsMatchContract() {
        // 契约第 10.1 节「可重试」列逐项断言
        assertThat(McpErrorCode.UNAUTHORIZED.isRetryable()).isTrue();
        assertThat(McpErrorCode.TOOL_NOT_FOUND.isRetryable()).isFalse();
        assertThat(McpErrorCode.INVALID_PARAMS.isRetryable()).isTrue();
        assertThat(McpErrorCode.ASSET_NOT_FOUND.isRetryable()).isFalse();
        assertThat(McpErrorCode.UPSTREAM_TIMEOUT.isRetryable()).isTrue();
        assertThat(McpErrorCode.UPSTREAM_UNAVAILABLE.isRetryable()).isTrue();
        assertThat(McpErrorCode.UPSTREAM_TOO_LARGE.isRetryable()).isFalse();
        assertThat(McpErrorCode.RATE_LIMITED.isRetryable()).isTrue();
        assertThat(McpErrorCode.INTERNAL_ERROR.isRetryable()).isTrue();
    }

    @Test
    void descriptionsAreNonEmptyAndStable() {
        for (McpErrorCode code : McpErrorCode.values()) {
            assertThat(code.getDescription()).isNotBlank();
        }
    }

    @Test
    void fromCodeRoundTrip() {
        for (McpErrorCode code : McpErrorCode.values()) {
            assertThat(McpErrorCode.fromCode(code.getCode())).isEqualTo(Optional.of(code));
        }
    }

    @Test
    void fromCodeReturnsEmptyForUnknownCode() {
        assertThat(McpErrorCode.fromCode("not-a-real-code")).isEmpty();
    }
}
