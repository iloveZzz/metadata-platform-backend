package com.yss.datamiddle.dqinsight.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 来源工具（冻结 OpenAPI SourceTool 枚举）。
 *
 * <p>外部 DQ 结果接入的来源工具：great-expectations（GE）/ generic（通用导入）。</p>
 */
public enum SourceTool {

    /** Great Expectations */
    GREAT_EXPECTATIONS("great-expectations"),
    /** 通用导入（CSV / API） */
    GENERIC("generic");

    private final String code;

    SourceTool(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static SourceTool fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (SourceTool value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static SourceTool fromCodeOrNull(String code) {
        return fromCode(code);
    }
}
