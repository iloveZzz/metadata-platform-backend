package com.yss.datamiddle.dqinsight.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 规则类型（冻结 OpenAPI RuleType 枚举；MVP 默认规则清单 OQ-02 / SB-02）。
 */
public enum RuleType {

    /** 非空率 */
    NON_NULL_RATE("non-null-rate"),
    /** 格式 */
    FORMAT("format"),
    /** 唯一性 */
    UNIQUENESS("uniqueness"),
    /** 值域 */
    VALUE_RANGE("value-range"),
    /** 新鲜度 */
    FRESHNESS("freshness");

    private final String code;

    RuleType(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static RuleType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (RuleType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static RuleType fromCodeOrNull(String code) {
        return fromCode(code);
    }
}
