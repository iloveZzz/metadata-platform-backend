package com.yss.datamiddle.semantic.metric.exception;

import com.yss.datamiddle.semantic.metric.model.MetricDefinition;

/**
 * 乐观锁版本冲突异常。
 */
public class MetricVersionConflictException extends RuntimeException {
    private final MetricDefinition latest;

    public MetricVersionConflictException(String message, MetricDefinition latest) {
        super(message);
        this.latest = latest;
    }

    public MetricDefinition getLatest() {
        return latest;
    }
}
