package com.yss.datamiddle.aicontextlayer.mcpserver.error;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 错误码映射测试（冻结契约第 10.1 节）：unauthorized / tool_not_found / internal_error 生效；
 * 未知异常统一映射 internal_error（SEC-11）。
 */
class McpErrorMapperTest {

    @Test
    void mapsUnauthorizedMcpException() {
        ErrorResponse response = McpErrorMapper.toErrorResponse(McpException.of(McpErrorCode.UNAUTHORIZED));

        assertThat(response.getCode()).isEqualTo("unauthorized");
        assertThat(response.isRetryable()).isTrue();
        assertThat(response.getMessage()).isNotBlank();
    }

    @Test
    void mapsToolNotFoundMcpException() {
        ErrorResponse response = McpErrorMapper.toErrorResponse(McpException.of(McpErrorCode.TOOL_NOT_FOUND));

        assertThat(response.getCode()).isEqualTo("tool_not_found");
        assertThat(response.isRetryable()).isFalse();
    }

    @Test
    void mapsUnknownRuntimeExceptionToInternalError() {
        ErrorResponse response = McpErrorMapper.toErrorResponse(new IllegalStateException("boom"));

        assertThat(response.getCode()).isEqualTo("internal_error");
        assertThat(response.isRetryable()).isTrue();
    }

    @Test
    void allNineCodesMappedThroughMapper() {
        for (McpErrorCode code : McpErrorCode.values()) {
            ErrorResponse response = McpErrorMapper.toErrorResponse(McpException.of(code));
            assertThat(response.getCode()).isEqualTo(code.getCode());
            assertThat(response.getMessage()).isNotBlank();
        }
    }
}
