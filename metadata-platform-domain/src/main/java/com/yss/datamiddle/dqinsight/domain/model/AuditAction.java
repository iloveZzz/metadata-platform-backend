package com.yss.datamiddle.dqinsight.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 审计动作（数据架构 §7，action 枚举 7 类）。
 */
public enum AuditAction {

    /** 接入（推送 / 拉取 / 解析成功） */
    INGEST("ingest"),
    /** 解析失败 */
    PARSE_FAIL("parse-fail"),
    /** 健康分计算 */
    HEALTH_CALC("health-calc"),
    /** 通道配置变更 */
    CHANNEL_CONFIG("channel-config"),
    /** 通道启停 */
    CHANNEL_TOGGLE("channel-toggle"),
    /** 通道重试拉取 */
    CHANNEL_RETRY("channel-retry"),
    /** 关联人工映射 */
    LINKAGE_MAP("linkage-map");

    private final String code;

    AuditAction(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static AuditAction fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AuditAction value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
