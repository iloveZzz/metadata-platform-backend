package com.yss.metadata.domain.collector.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 采集实例执行状态。
 */
@Getter
@AllArgsConstructor
public enum CollectorInstanceStatus {

    PENDING("pending", "等待中"),
    RUNNING("running", "运行中"),
    SUCCESS("success", "成功"),
    FAILED("failed", "失败");

    @JsonValue
    private final String code;
    private final String description;

    public static CollectorInstanceStatus fromCode(String code) {
        if (code == null) return null;
        for (CollectorInstanceStatus status : values()) {
            if (status.code.equalsIgnoreCase(code) || status.name().equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null;
    }
}
