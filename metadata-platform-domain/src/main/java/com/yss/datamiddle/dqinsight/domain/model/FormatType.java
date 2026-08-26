package com.yss.datamiddle.dqinsight.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 格式类型（冻结 OpenAPI FormatType 枚举）。
 *
 * <p>ge（GE 结果 JSON）/ csv（通用 CSV 导入）/ api（通用 API 结果）。预留 dbt / profiler 扩展位（DQI-009）。</p>
 */
public enum FormatType {

    /** GE 结果（application/json，DQResultSubmit） */
    GE("ge"),
    /** 通用 CSV 导入（SB-04 schema） */
    CSV("csv"),
    /** 通用 API 结果（application/json，DQResultSubmit） */
    API("api");

    private final String code;

    FormatType(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static FormatType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (FormatType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static FormatType fromCodeOrNull(String code) {
        return fromCode(code);
    }
}
