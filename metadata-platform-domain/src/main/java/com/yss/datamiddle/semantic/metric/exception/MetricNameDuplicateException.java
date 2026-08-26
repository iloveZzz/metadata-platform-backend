package com.yss.datamiddle.semantic.metric.exception;

import com.yss.datamiddle.semantic.term.exception.BusinessValidationException;

public class MetricNameDuplicateException extends BusinessValidationException {
    public MetricNameDuplicateException(String name) {
        super("PARAM_VALIDATION_ERROR", "name", "METRIC_NAME_DUPLICATE", "指标口径名称已存在: " + name);
    }
}
