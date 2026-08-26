package com.yss.metadata.domain.lineage.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 血缘类型（冻结 OpenAPI 枚举：sql/job/manual）。
 */
public enum LineageType {

    /** SQL 解析来源 */
    SQL("sql", "SQL"),

    /** 作业/调度来源 */
    JOB("job", "作业"),

    /** 人工补录来源 */
    MANUAL("manual", "人工");

    private final String value;

    private final String description;

    LineageType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 列值 → 枚举；未知值抛非法参数（由 Web 层统一映射 422）。
     */
    @JsonCreator
    public static LineageType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (LineageType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知血缘类型: " + value);
    }
}
