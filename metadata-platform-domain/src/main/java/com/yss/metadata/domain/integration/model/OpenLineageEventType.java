package com.yss.metadata.domain.integration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * OpenLineage 事件类型（冻结 API OpenLineageEvent.eventType 枚举）。
 *
 * <p>JSON 序列化/反序列化使用 value（对齐 ConnectorType 约定）。</p>
 */
public enum OpenLineageEventType {

    /** Run 开始 */
    START("START", "开始"),

    /** Run 完成（携带 inputs/outputs，产出血缘） */
    COMPLETE("COMPLETE", "完成"),

    /** Run 失败 */
    FAIL("FAIL", "失败"),

    /** Run 中止 */
    ABORT("ABORT", "中止");

    private final String value;

    private final String description;

    OpenLineageEventType(String value, String description) {
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
    public static OpenLineageEventType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (OpenLineageEventType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知 OpenLineage 事件类型: " + value);
    }
}
