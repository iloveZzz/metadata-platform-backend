package com.yss.datamiddle.dqinsight.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 档位 / 独立展示态筛选（冻结 OpenAPI BandFilter 枚举）。
 *
 * <p>优 / 良 / 差为健康分档位（OQ-01）；expired / noresult 为独立展示态筛选，与档位并列，
 * 不归入档位（SB-01 / C23）。</p>
 */
public enum BandFilter {

    /** 档位 = 优 */
    GOOD("优"),
    /** 档位 = 良 */
    FAIR("良"),
    /** 档位 = 差 */
    POOR("差"),
    /** 过期（独立展示态） */
    EXPIRED("expired"),
    /** 无结果（独立展示态） */
    NORESULT("noresult");

    private final String code;

    BandFilter(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static BandFilter fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (BandFilter value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static BandFilter fromCodeOrNull(String code) {
        return fromCode(code);
    }
}
