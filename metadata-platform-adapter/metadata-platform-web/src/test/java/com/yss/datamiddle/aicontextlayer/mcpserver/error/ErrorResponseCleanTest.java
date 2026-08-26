package com.yss.datamiddle.aicontextlayer.mcpserver.error;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 清洁错误响应测试（SEC-11）：internal_error 响应不含堆栈、内部字段名、内部配置、凭据；
 * 日志 / 响应面不泄漏实现细节。
 */
class ErrorResponseCleanTest {

    private static final List<String> STACK_TRACE_MARKERS = Arrays.asList(
        " at com.yss.", "Exception at", "Caused by", "stacktrace", "at java.base"
    );

    private static final List<String> INTERNAL_FIELD_NAME_MARKERS = Arrays.asList(
        "presentedSecret", "credential_ref", "credentialsBySecret", "InternalField", "datasource"
    );

    private static final List<String> CREDENTIAL_MARKERS = Arrays.asList(
        "password", "secret-token", "api_key_value"
    );

    @Test
    void internalErrorResponseContainsNoStackTraceOrInternalFieldNames() {
        ErrorResponse response = McpErrorMapper.toErrorResponse(
            new IllegalStateException("内部异常：com.yss.datamiddle.aicontextlayer.mcpserver.McpServer#handleConnection"));

        assertThat(response.getCode()).isEqualTo("internal_error");
        assertThat(response.getMessage()).isNotBlank();
        assertNoLeakMarkers(response);
    }

    @Test
    void exceptionMessageNeverLeaksIntoResponse() {
        // 异常消息即使包含堆栈 / 内部字段名 / 凭据，也不得进入响应
        ErrorResponse response = McpErrorMapper.toErrorResponse(
            new IllegalStateException("ERROR: presentedSecret=abc at com.yss.internal.Class line 42"));

        assertThat(response.getCode()).isEqualTo("internal_error");
        assertNoLeakMarkers(response);
    }

    @Test
    void unauthorizedResponseCarriesFrozenDescriptionOnly() {
        ErrorResponse response = McpErrorMapper.toErrorResponse(McpException.of(McpErrorCode.UNAUTHORIZED));

        assertThat(response.getCode()).isEqualTo("unauthorized");
        assertNoLeakMarkers(response);
    }

    private static void assertNoLeakMarkers(ErrorResponse response) {
        String message = response.getMessage().toLowerCase();
        for (String marker : STACK_TRACE_MARKERS) {
            assertThat(message).as("响应不得包含堆栈标记 [%s]", marker)
                .doesNotContain(marker.toLowerCase());
        }
        for (String marker : INTERNAL_FIELD_NAME_MARKERS) {
            assertThat(message).as("响应不得包含内部字段名 [%s]", marker)
                .doesNotContain(marker.toLowerCase());
        }
        for (String marker : CREDENTIAL_MARKERS) {
            assertThat(message).as("响应不得包含凭据内容 [%s]", marker)
                .doesNotContain(marker.toLowerCase());
        }
    }
}
