package com.yss.metadata.domain.collector.model;

/**
 * 采集任务状态（spec FR-005 / 系统概要设计 §5 状态机）。
 */
public enum CollectorTaskStatus {

    /** 待执行：创建后初始状态 */
    PENDING("pending", "待执行"),
    /** 运行中：调度/手动触发进入，运行中拒绝重复触发（幂等） */
    RUNNING("running", "运行中"),
    /** 成功 */
    SUCCESS("success", "成功"),
    /** 失败：携带失败原因，支持局部重采语义 */
    FAILED("failed", "失败"),
    /** 已取消：仅运行中可取消 */
    CANCELLED("cancelled", "已取消");

    private final String value;

    private final String description;

    CollectorTaskStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
