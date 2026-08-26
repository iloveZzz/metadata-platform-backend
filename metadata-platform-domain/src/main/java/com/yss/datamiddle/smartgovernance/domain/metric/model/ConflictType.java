package com.yss.datamiddle.smartgovernance.domain.metric.model;

/**
 * 指标冲突类型枚举
 */
public enum ConflictType {
    SYNONYMOUS_NAME("SYNONYMOUS_NAME", "同义异名 (计算逻辑等价)"),
    HOMONYMOUS_MEANING("HOMONYMOUS_MEANING", "同名异义 (名称相同但逻辑差异)"),
    FORMULA_DRIFT("FORMULA_DRIFT", "口径漂移 (过滤条件/时间窗口微小差异)");

    private final String code;
    private final String label;

    ConflictType(String code, String label) {
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
