package com.yss.datamiddle.dqinsight.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 审计结果（success / failure）。
 */
public enum AuditResult {

    /** 成功 */
    SUCCESS("success"),
    /** 失败 */
    FAILURE("failure");

    private final String code;

    AuditResult(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static AuditResult fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AuditResult value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
