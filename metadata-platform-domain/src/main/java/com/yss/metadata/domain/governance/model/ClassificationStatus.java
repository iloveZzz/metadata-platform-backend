package com.yss.metadata.domain.governance.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 分级分类结果状态（classification.status：待确认/已确认/已修正）。
 *
 * <p>JSON 序列化/反序列化使用 value（对齐 ConnectorType 约定）。</p>
 */
public enum ClassificationStatus {

    /** 待确认（自动识别候选） */
    PENDING("pending", "待确认"),

    /** 已确认（治理专员确认候选分类） */
    CONFIRMED("confirmed", "已确认"),

    /** 已修正（治理专员修正候选分类） */
    CORRECTED("corrected", "已修正");

    private final String value;

    private final String description;

    ClassificationStatus(String value, String description) {
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
    public static ClassificationStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (ClassificationStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知分类状态: " + value);
    }
}
