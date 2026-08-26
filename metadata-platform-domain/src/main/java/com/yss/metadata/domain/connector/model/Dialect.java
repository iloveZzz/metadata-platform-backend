package com.yss.metadata.domain.connector.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 连接器方言（冻结 OpenAPI ConnectorCreate.dialect 枚举）。
 *
 * <p>JSON 序列化/反序列化使用 value（如 "mysql-compatible"）。</p>
 */
public enum Dialect {

    NATIVE("native"),
    MYSQL_COMPATIBLE("mysql-compatible"),
    GAUSSDB("gaussdb"),
    AUTO("auto");

    private final String value;

    Dialect(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Dialect fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (Dialect dialect : values()) {
            if (dialect.value.equals(value)) {
                return dialect;
            }
        }
        throw new IllegalArgumentException("未知方言: " + value);
    }
}
