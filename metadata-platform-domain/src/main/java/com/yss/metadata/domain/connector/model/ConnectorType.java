package com.yss.metadata.domain.connector.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 连接器类型（冻结 OpenAPI ConnectorCreate.type 枚举）。
 *
 * <p>与 OpenAPI 枚举值保持一致，JSON 序列化/反序列化使用 value（如 "OSS/S3"）。</p>
 */
public enum ConnectorType {

    MYSQL("MySQL"),
    ORACLE("Oracle"),
    OCEANBASE("OceanBase"),
    GAUSSDB("GaussDB"),
    DORIS("Doris"),
    STARROCKS("StarRocks"),
    ICEBERG("Iceberg"),
    HUDI("Hudi"),
    PAIMON("Paimon"),
    OSS_S3("OSS/S3");

    private final String value;

    ConnectorType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ConnectorType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (ConnectorType type : values()) {
            if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)
                    || (type == OSS_S3 && ("OSS_S3".equalsIgnoreCase(value) || "OSS/S3".equalsIgnoreCase(value)))) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知连接器类型: " + value);
    }
}
