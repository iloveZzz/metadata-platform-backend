package com.yss.metadata.domain.collector.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 采集实例执行方式。
 */
@Getter
@AllArgsConstructor
public enum ExecutionMode {

    MANUAL("manual", "手动触发"),
    SCHEDULE("schedule", "定时调度"),
    AUTO_RETRY("auto_retry", "自动重试"),
    DRY_RUN("dry_run", "空跑");

    @JsonValue
    private final String code;
    private final String description;

    public static ExecutionMode fromCode(String code) {
        if (code == null) return null;
        for (ExecutionMode mode : values()) {
            if (mode.code.equalsIgnoreCase(code) || mode.name().equalsIgnoreCase(code)) {
                return mode;
            }
        }
        return null;
    }
}
