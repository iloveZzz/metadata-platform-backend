package com.yss.datamiddle.dqinsight.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 健康分状态（冻结 OpenAPI HealthState 枚举）。
 *
 * <p>ok=已计算档位；expired=过期独立展示态（标灰 + 提示重新接入，OQ-03）；noresult=无结果独立展示态
 * （从未接入 / 接入失败，不归入档位）；calculating=计算中。过期态由查询投影派生（validUntil &lt; now），
 * 与「无结果」独立展示态不混淆（C23）。</p>
 */
public enum HealthState {

    /** 已计算（档位：优 / 良 / 差） */
    OK("ok"),
    /** 过期（独立展示态，系统自动流转，非用户动作） */
    EXPIRED("expired"),
    /** 无结果（独立展示态，从未接入 / 接入失败） */
    NORESULT("noresult"),
    /** 计算中 */
    CALCULATING("calculating");

    private final String code;

    HealthState(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static HealthState fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (HealthState value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static HealthState fromCodeOrNull(String code) {
        return fromCode(code);
    }
}
