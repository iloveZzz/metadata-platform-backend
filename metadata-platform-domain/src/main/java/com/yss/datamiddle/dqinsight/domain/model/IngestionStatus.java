package com.yss.datamiddle.dqinsight.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 接入状态（冻结 OpenAPI IngestionStatus 枚举）。
 *
 * <p>processing=待处理 / ingested=已入库 / parse-failed=解析失败 / invalidated=已失效（结果过期，OQ-03 系统自动流转）。</p>
 */
public enum IngestionStatus {

    /** 待处理 */
    PROCESSING("processing"),
    /** 已入库（解析成功即入库，与关联解耦） */
    INGESTED("ingested"),
    /** 解析失败（错误分类 format / auth / network） */
    PARSE_FAILED("parse-failed"),
    /** 已失效（结果超过有效期，系统自动流转） */
    INVALIDATED("invalidated");

    private final String code;

    IngestionStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static IngestionStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (IngestionStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static IngestionStatus fromCodeOrNull(String code) {
        return fromCode(code);
    }
}
