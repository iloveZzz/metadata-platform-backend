package com.yss.datamiddle.smartgovernance.domain.metric.model;

/**
 * 指标冲突事件处理状态枚举
 */
public enum ConflictStatus {
    UNRESOLVED("UNRESOLVED", "未处理 (待治理)"),
    RESOLVED("RESOLVED", "已解决 (对齐归并)"),
    SUSPECTED("SUSPECTED", "口径存疑 (警示流转)"),
    DISMISSED("DISMISSED", "已忽略 (确认误报)");

    private final String code;
    private final String label;

    ConflictStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
