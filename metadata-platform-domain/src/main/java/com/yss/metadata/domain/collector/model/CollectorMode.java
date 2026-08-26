package com.yss.metadata.domain.collector.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 采集模式（冻结 OpenAPI CollectorCreate.mode 枚举）。
 */
public enum CollectorMode {

    /** 增量采集 */
    INCREMENTAL("incremental"),
    /** 全量采集 */
    FULL("full");

    private final String value;

    CollectorMode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CollectorMode fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (CollectorMode mode : values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("未知采集模式: " + value);
    }
}
