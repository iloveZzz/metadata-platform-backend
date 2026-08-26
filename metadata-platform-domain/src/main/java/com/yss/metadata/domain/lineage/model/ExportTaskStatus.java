package com.yss.metadata.domain.lineage.model;

/**
 * 导出任务状态（export_task.status：pending/running/success/failed）。
 */
public enum ExportTaskStatus {

    /** 已创建未开始 */
    PENDING("pending", "待执行"),

    /** 生成中 */
    RUNNING("running", "生成中"),

    /** 完成（file_ref 可下载） */
    SUCCESS("success", "成功"),

    /** 失败 */
    FAILED("failed", "失败");

    private final String value;

    private final String description;

    ExportTaskStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 列值 → 枚举；未知值抛非法参数（由 Web 层统一映射 422）。
     */
    public static ExportTaskStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (ExportTaskStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知导出任务状态: " + value);
    }
}
