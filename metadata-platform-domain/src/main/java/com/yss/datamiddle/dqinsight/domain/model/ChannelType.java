package com.yss.datamiddle.dqinsight.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 接入通道类型（冻结 OpenAPI Channel.type：api-push / scheduled-pull）。
 */
public enum ChannelType {

    /** API 推送（外部系统主动推送） */
    API_PUSH("api-push"),

    /** 定时拉取（按周期取回结果，复用切片 01 接入管线） */
    SCHEDULED_PULL("scheduled-pull");

    private final String code;

    ChannelType(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static ChannelType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ChannelType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static ChannelType fromCodeOrNull(String code) {
        return fromCode(code);
    }
}
