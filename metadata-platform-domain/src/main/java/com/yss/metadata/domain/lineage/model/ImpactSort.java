package com.yss.metadata.domain.lineage.model;

/**
 * 影响分析排序键（冻结 OpenAPI 枚举：depth/domain/risk，默认 depth）。
 */
public enum ImpactSort {

    /** 按影响深度（分组内按名称） */
    DEPTH("depth", "影响深度"),

    /** 按数据域 */
    DOMAIN("domain", "数据域"),

    /** 按风险（由分类推导，敏感高优先） */
    RISK("risk", "风险");

    private final String value;

    private final String description;

    ImpactSort(String value, String description) {
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
    public static ImpactSort fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DEPTH;
        }
        for (ImpactSort sort : values()) {
            if (sort.value.equals(value)) {
                return sort;
            }
        }
        throw new IllegalArgumentException("未知影响分析排序: " + value);
    }
}
