package com.yss.datamiddle.semantic.metric.exception;

public class MetricNotFoundException extends RuntimeException {
    public MetricNotFoundException(Long id) {
        super("指标口径不存在: " + id);
    }
}
