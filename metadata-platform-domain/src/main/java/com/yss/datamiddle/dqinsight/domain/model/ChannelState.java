package com.yss.datamiddle.dqinsight.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 接入通道状态（冻结 OpenAPI Channel.state：enabled / disabled / pulling / pull-failed）。
 *
 * <p>状态机：启用 / 停用 ↔ 拉取中 ↔ 拉取失败（C25，状态流转领域规则在 IngestionChannel，
 * 越界流转抛 ChannelBusyException）。</p>
 */
public enum ChannelState {

    /** 启用（接受 API 推送，按周期定时拉取） */
    ENABLED("enabled"),

    /** 停用（拒绝推送，停止拉取；停用需二次确认） */
    DISABLED("disabled"),

    /** 拉取中（定时触发 / 手动重试；幂等，重复触发 409 busy） */
    PULLING("pulling"),

    /** 拉取失败（网络 / 认证 / 格式分类，展示错误信息 + 重试入口） */
    PULL_FAILED("pull-failed");

    private final String code;

    ChannelState(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static ChannelState fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ChannelState value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static ChannelState fromCodeOrNull(String code) {
        return fromCode(code);
    }
}
