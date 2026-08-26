package com.yss.metadata.application.collector.support;

import com.yss.metadata.domain.collector.model.CollectorExecutionResult;
import com.yss.metadata.domain.collector.model.CollectorTask;
import com.yss.metadata.domain.collector.spi.CollectorExecutionSpi;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 采集执行 SPI 测试替身（seam-deferred）。
 *
 * <p>按测试场景预先编排返回结果（成功 / 失败携带原因），
 * 替代真实外部数据源物理采集执行。</p>
 */
public class FakeCollectorExecutionSpi implements CollectorExecutionSpi {

    private final Deque<CollectorExecutionResult> results = new ArrayDeque<>();

    public void enqueue(CollectorExecutionResult result) {
        results.addLast(result);
    }

    @Override
    public CollectorExecutionResult execute(CollectorTask task) {
        if (results.isEmpty()) {
            return CollectorExecutionResult.success();
        }
        return results.removeFirst();
    }
}
