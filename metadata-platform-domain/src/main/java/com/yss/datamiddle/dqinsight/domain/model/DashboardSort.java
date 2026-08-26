package com.yss.datamiddle.dqinsight.domain.model;

/**
 * 仪表盘资产列表排序字段（冻结 OpenAPI sort 枚举：score / lastResultAt / name，默认 score）。
 *
 * <p>方向约定（freeze 记录 m8：无 sortDir 参数，本包实现决策）：score 降序（高分优先）、
 * lastResultAt 降序（最近结果优先，与列表既有默认一致）、name 升序（名称 A→Z）；
 * 排序语义为切片 03 人工审查点（性能 / 展示一致性）。</p>
 */
public enum DashboardSort {

    /** 按健康分降序（默认） */
    SCORE("score"),

    /** 按最近结果时间降序 */
    LAST_RESULT_AT("lastResultAt"),

    /** 按资产名称升序 */
    NAME("name");

    private final String code;

    DashboardSort(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * 按契约码解析；空 / 未知返回 null（调用方回退默认排序）。
     */
    public static DashboardSort fromCodeOrNull(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        for (DashboardSort value : values()) {
            if (value.code.equals(code.trim())) {
                return value;
            }
        }
        return null;
    }
}
