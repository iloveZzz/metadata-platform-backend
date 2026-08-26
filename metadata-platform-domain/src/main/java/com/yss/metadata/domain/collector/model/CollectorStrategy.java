package com.yss.metadata.domain.collector.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 采集覆盖策略（冻结 OpenAPI CollectorCreate.strategy 枚举）。
 */
public enum CollectorStrategy {

    /** 冲突忽略 */
    IGNORE("ignore"),
    /** 冲突覆盖 */
    OVERWRITE("overwrite"),
    /** 失败中止 */
    ABORT_ON_FAILURE("abort-on-failure");

    private final String value;

    CollectorStrategy(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CollectorStrategy fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (CollectorStrategy strategy : values()) {
            if (strategy.value.equals(value)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("未知采集策略: " + value);
    }
}
