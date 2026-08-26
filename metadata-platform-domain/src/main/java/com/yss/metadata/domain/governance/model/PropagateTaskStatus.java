package com.yss.metadata.domain.governance.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 分类传播任务状态（propagate_task.status：pending/running/success/failed）。
 *
 * <p>JSON 序列化/反序列化使用 value（对齐 ConnectorType 约定）。</p>
 */
public enum PropagateTaskStatus {

    /** 已创建未开始 */
    PENDING("pending", "待执行"),

    /** 传播中 */
    RUNNING("running", "传播中"),

    /** 完成（coverage 可核验） */
    SUCCESS("success", "成功"),

    /** 失败 */
    FAILED("failed", "失败");

    private final String value;

    private final String description;

    PropagateTaskStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 列值 → 枚举；未知值抛非法参数（由 Web 层统一映射 422）。
     */
    @JsonCreator
    public static PropagateTaskStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (PropagateTaskStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知传播任务状态: " + value);
    }
}
