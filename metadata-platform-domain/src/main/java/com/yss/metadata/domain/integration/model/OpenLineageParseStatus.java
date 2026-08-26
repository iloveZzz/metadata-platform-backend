package com.yss.metadata.domain.integration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * OpenLineage 事件解析状态（openlineage_event.parse_status）。
 *
 * <p>JSON 序列化/反序列化使用 value（对齐 ConnectorType 约定）。</p>
 */
public enum OpenLineageParseStatus {

    /** 已接收（未解析/无需解析） */
    RECEIVED("received", "已接收"),

    /** 解析成功（已写入资产+血缘） */
    PARSED("parsed", "解析成功"),

    /** 解析失败（事件缺必填/数据集非法） */
    PARSE_FAILED("parse_failed", "解析失败");

    private final String value;

    private final String description;

    OpenLineageParseStatus(String value, String description) {
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
     * 列值 → 枚举；未知值抛非法参数。
     */
    @JsonCreator
    public static OpenLineageParseStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (OpenLineageParseStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知 OpenLineage 解析状态: " + value);
    }
}
