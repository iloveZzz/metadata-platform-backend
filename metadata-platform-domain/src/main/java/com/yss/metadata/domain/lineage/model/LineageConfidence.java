package com.yss.metadata.domain.lineage.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 血缘置信度（冻结 OpenAPI 枚举：auto-high/auto-mid/manual-high/low）。
 *
 * <p>业务规则：置信度显式标识，不隐式提升（数据架构 §3）——
 * 解析器/采集器写 auto-*，人工补录默认 manual-high，低置信来源写 low。</p>
 */
public enum LineageConfidence {

    /** 自动解析-高置信 */
    AUTO_HIGH("auto-high", "自动-高"),

    /** 自动解析-中置信 */
    AUTO_MID("auto-mid", "自动-中"),

    /** 人工补录-高置信 */
    MANUAL_HIGH("manual-high", "人工-高"),

    /** 低置信来源 */
    LOW("low", "低置信来源");

    private final String value;

    private final String description;

    LineageConfidence(String value, String description) {
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
    public static LineageConfidence fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (LineageConfidence confidence : values()) {
            if (confidence.value.equals(value)) {
                return confidence;
            }
        }
        throw new IllegalArgumentException("未知置信度: " + value);
    }
}
