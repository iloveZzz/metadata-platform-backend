package com.yss.datamiddle.dqinsight.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 关联状态（冻结 OpenAPI LinkageState 枚举）。
 *
 * <p>linked=已关联 / pending=未命中（待关联队列）/ none=不适用（无资产关联信息）。</p>
 */
public enum LinkageState {

    /** 已关联 */
    LINKED("linked"),
    /** 未命中（待关联队列，人工映射属切片 04） */
    PENDING("pending"),
    /** 不适用（解析失败或无资产关联信息） */
    NONE("none");

    private final String code;

    LinkageState(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static LinkageState fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (LinkageState value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static LinkageState fromCodeOrNull(String code) {
        return fromCode(code);
    }

    /**
     * 聚合批次关联状态：任一已关联 → linked；否则任一未命中 → pending；否则 none。
     *
     * @param linkages 该批次的资产关联列表
     * @return 批次级关联状态
     */
    public static LinkageState resolve(java.util.List<AssetLinkage> linkages) {
        boolean anyLinked = false;
        boolean anyPending = false;
        if (linkages != null) {
            for (AssetLinkage linkage : linkages) {
                if (linkage.getState() == LINKED) {
                    anyLinked = true;
                } else if (linkage.getState() == PENDING) {
                    anyPending = true;
                }
            }
        }
        if (anyLinked) {
            return LINKED;
        }
        if (anyPending) {
            return PENDING;
        }
        return NONE;
    }
}
