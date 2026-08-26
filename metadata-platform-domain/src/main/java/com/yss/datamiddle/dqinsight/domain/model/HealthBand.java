package com.yss.datamiddle.dqinsight.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 健康分档位（冻结 OpenAPI HealthBand 枚举，OQ-01 / SB-01 已确认：≥90 优 / 75~89 良 / <75 差）。
 *
 * <p>档位为健康分模型一等概念；无结果 / 过期是独立展示态，不归入档位（阈值常量与边界测试见 HealthScoreEngine）。</p>
 */
public enum HealthBand {

    /** 优（可放心使用） */
    GOOD("优"),
    /** 良（存在少量问题） */
    FAIR("良"),
    /** 差（不建议直接使用） */
    POOR("差");

    private final String code;

    HealthBand(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static HealthBand fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (HealthBand value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static HealthBand fromCodeOrNull(String code) {
        return fromCode(code);
    }
}
