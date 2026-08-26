package com.yss.datamiddle.dqinsight.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 规则结果状态（冻结 OpenAPI RuleStatus 枚举）。
 *
 * <p>参与健康分计算：passed=100 / warn=80 / failed|error=0（SB-02，切片 02 消费）。</p>
 */
public enum RuleStatus {

    /** 通过 */
    PASSED("passed"),
    /** 警告 */
    WARN("warn"),
    /** 失败 */
    FAILED("failed"),
    /** 错误 */
    ERROR("error");

    private final String code;

    RuleStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static RuleStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (RuleStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static RuleStatus fromCodeOrNull(String code) {
        return fromCode(code);
    }
}
