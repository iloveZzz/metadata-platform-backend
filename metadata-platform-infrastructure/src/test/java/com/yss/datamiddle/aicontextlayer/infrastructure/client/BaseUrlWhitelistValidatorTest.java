package com.yss.datamiddle.aicontextlayer.infrastructure.client;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BaseUrlWhitelistValidatorTest {

    private final BaseUrlWhitelistValidator validator = new BaseUrlWhitelistValidator();

    @Test
    @DisplayName("白名单内 URL 校验通过")
    void allowedUrlsPassValidation() {
        assertDoesNotThrow(() -> validator.validate("http://localhost:8080"));
        assertDoesNotThrow(() -> validator.validate("http://127.0.0.1:8080/api"));
        assertDoesNotThrow(() -> validator.validate("https://metadata-platform/api"));
    }

    @Test
    @DisplayName("云元数据端点被拦截（SSRF 防御）")
    void cloudMetadataEndpointBlocked() {
        McpException ex = assertThrows(McpException.class, () ->
            validator.validate("http://169.254.169.254/latest/meta-data")
        );
        assertEquals(McpErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    @DisplayName("非白名单外部主机被拦截")
    void nonWhitelistHostBlocked() {
        McpException ex = assertThrows(McpException.class, () ->
            validator.validate("https://attacker.com/evil")
        );
        assertEquals(McpErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    @DisplayName("非 HTTP/HTTPS 协议被拦截")
    void nonHttpSchemeBlocked() {
        McpException ex = assertThrows(McpException.class, () ->
            validator.validate("file:///etc/passwd")
        );
        assertEquals(McpErrorCode.INVALID_PARAMS, ex.getErrorCode());
    }
}
