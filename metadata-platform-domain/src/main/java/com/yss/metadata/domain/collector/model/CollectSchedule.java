package com.yss.metadata.domain.collector.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 采集调度值对象（冻结 OpenAPI CollectorCreate.schedule：cron 或周期描述）。
 *
 * <p>按值相等（value object），表达式非空。</p>
 */
@Getter
@EqualsAndHashCode
@ToString
public final class CollectSchedule implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 调度表达式（cron 或周期描述） */
    private final String expression;

    public CollectSchedule(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("调度表达式不能为空");
        }
        this.expression = expression;
    }

    public String getValue() {
        return expression;
    }
}
