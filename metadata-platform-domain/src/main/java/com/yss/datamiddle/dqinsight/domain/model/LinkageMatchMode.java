package com.yss.datamiddle.dqinsight.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 关联匹配方式（数据架构 §5 dq_asset_linkage.match_mode）。
 */
public enum LinkageMatchMode {

    /** 自动匹配（资产 ID 精确命中主平台资产） */
    AUTO("auto"),
    /** 人工映射（切片 04） */
    MANUAL("manual");

    private final String code;

    LinkageMatchMode(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static LinkageMatchMode fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (LinkageMatchMode value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
