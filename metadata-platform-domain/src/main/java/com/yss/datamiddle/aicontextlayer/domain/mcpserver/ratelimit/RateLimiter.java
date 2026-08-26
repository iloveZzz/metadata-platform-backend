package com.yss.datamiddle.aicontextlayer.domain.mcpserver.ratelimit;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP Agent 请求限流器（SEC-07 / 断言 5）。
 *
 * <p>默认单 Agent 20 QPS（突发 40）。超限抛出 {@link McpErrorCode#RATE_LIMITED}。</p>
 */
public class RateLimiter {

    public static final int DEFAULT_MAX_QPS = 20;
    public static final int DEFAULT_BURST = 40;

    private final int maxQps;
    private final ConcurrentHashMap<String, SlidingWindowCounter> counterMap = new ConcurrentHashMap<>();

    public RateLimiter() {
        this(DEFAULT_MAX_QPS);
    }

    public RateLimiter(int maxQps) {
        this.maxQps = maxQps;
    }

    /**
     * 尝试获取令牌，超限抛出 RATE_LIMITED。
     *
     * @param agentId Agent 唯一标识
     */
    public void acquire(String agentId) {
        if (agentId == null || agentId.trim().isEmpty()) {
            return;
        }
        SlidingWindowCounter counter = counterMap.computeIfAbsent(agentId, k -> new SlidingWindowCounter(maxQps));
        if (!counter.tryAcquire()) {
            throw McpException.of(McpErrorCode.RATE_LIMITED);
        }
    }

    private static class SlidingWindowCounter {
        private final int limit;
        private long currentWindowStart;
        private final AtomicInteger count = new AtomicInteger(0);

        public SlidingWindowCounter(int limit) {
            this.limit = limit;
            this.currentWindowStart = System.currentTimeMillis() / 1000;
        }

        public synchronized boolean tryAcquire() {
            long nowSec = System.currentTimeMillis() / 1000;
            if (nowSec > currentWindowStart) {
                currentWindowStart = nowSec;
                count.set(0);
            }
            if (count.incrementAndGet() > limit) {
                return false;
            }
            return true;
        }
    }
}
