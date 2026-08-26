package com.yss.datamiddle.aicontextlayer.domain.mcpserver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MCP protocolVersion 协商与 transport 子集对齐测试（冻结契约第 11 节 SB-01）：
 * 冻结 revision 方向 "2025-06-18" 为支持版本；缺省请求回退到支持版本；不兼容版本协商失败。
 */
class McpProtocolVersionTest {

    @Test
    void usesSupportedVersionWhenRequestedIsNull() {
        McpProtocolVersion.NegotiationResult result = McpProtocolVersion.negotiate(null);
        assertThat(result.isAccepted()).isTrue();
        assertThat(result.getNegotiated()).isEqualTo(McpProtocolVersion.SUPPORTED);
    }

    @Test
    void usesSupportedVersionWhenRequestedIsBlank() {
        McpProtocolVersion.NegotiationResult result = McpProtocolVersion.negotiate("  ");
        assertThat(result.isAccepted()).isTrue();
        assertThat(result.getNegotiated()).isEqualTo(McpProtocolVersion.SUPPORTED);
    }

    @Test
    void acceptsSupportedVersion() {
        McpProtocolVersion.NegotiationResult result =
            McpProtocolVersion.negotiate(McpProtocolVersion.SUPPORTED);
        assertThat(result.isAccepted()).isTrue();
        assertThat(result.getNegotiated()).isEqualTo(McpProtocolVersion.SUPPORTED);
    }

    @Test
    void rejectsUnsupportedVersion() {
        McpProtocolVersion.NegotiationResult result = McpProtocolVersion.negotiate("2030-01-01");
        assertThat(result.isAccepted()).isFalse();
        assertThat(result.getRequested()).isEqualTo("2030-01-01");
    }
}
