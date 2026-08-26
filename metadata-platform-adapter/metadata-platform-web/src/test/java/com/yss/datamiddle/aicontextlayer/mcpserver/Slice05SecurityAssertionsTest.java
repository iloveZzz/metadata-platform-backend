package com.yss.datamiddle.aicontextlayer.mcpserver;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ratelimit.RateLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Slice05SecurityAssertionsTest {

    @Test
    @DisplayName("断言 5: 单 Agent 超频触发 rate_limited 限流")
    void rateLimitingTriggersRateLimitedException() {
        RateLimiter limiter = new RateLimiter(5); // 5 QPS 测试窗口
        String agentId = "agent-fast";

        // 前 5 次成功
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> limiter.acquire(agentId));
        }

        // 第 6 次触发 rate_limited
        McpException ex = assertThrows(McpException.class, () -> limiter.acquire(agentId));
        assertEquals(McpErrorCode.RATE_LIMITED, ex.getErrorCode());
    }
}
