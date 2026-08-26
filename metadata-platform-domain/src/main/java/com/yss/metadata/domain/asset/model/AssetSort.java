package com.yss.metadata.domain.asset.model;

/**
 * 资产搜索排序枚举（冻结 OpenAPI GET /api/assets sort，默认 updatedAt 倒序）。
 *
 * <p>方向策略：updatedAt 倒序（数据架构/交互说明默认「更新时间倒序」）；
 * name / classification 升序（目录排序常规语义，原型 sorter 行为一致）。
 * 冻结契约未暴露方向参数，方向固定于本枚举映射。</p>
 */
public enum AssetSort {

    /** 默认：按最后更新时间倒序 */
    UPDATED_AT("updatedAt"),

    /** 按名称升序 */
    NAME("name"),

    /** 按分级分类升序 */
    CLASSIFICATION("classification");

    private final String value;

    AssetSort(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 请求值 → 枚举；缺省或空返回默认 updatedAt；
     * 未知值抛非法参数（由 Web 层统一映射 422）。
     */
    public static AssetSort fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return UPDATED_AT;
        }
        for (AssetSort sort : values()) {
            if (sort.value.equals(value)) {
                return sort;
            }
        }
        throw new IllegalArgumentException("不支持的排序字段: " + value);
    }
}
