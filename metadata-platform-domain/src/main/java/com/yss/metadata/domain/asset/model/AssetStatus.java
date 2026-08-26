package com.yss.metadata.domain.asset.model;

/**
 * 资产状态（数据架构 asset.status；状态矩阵：待认领/已认领/已归档/已删除）。
 *
 * <p>列存储值与冻结 OpenAPI 枚举一致（小写字符串）；
 * 「已删除」由采集在源端删除时标记（区别于归档，见状态矩阵决策）。</p>
 */
public enum AssetStatus {

    /** 待认领（owner 为空） */
    PENDING("pending", "待认领"),

    /** 已认领（owner 唯一） */
    CLAIMED("claimed", "已认领"),

    /** 已归档（只读；取消归档恢复） */
    ARCHIVED("archived", "已归档"),

    /** 已删除（源端删除标记；只读） */
    DELETED("deleted", "已删除");

    private final String value;

    private final String description;

    AssetStatus(String value, String description) {
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
    public static AssetStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (AssetStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知资产状态: " + value);
    }
}
