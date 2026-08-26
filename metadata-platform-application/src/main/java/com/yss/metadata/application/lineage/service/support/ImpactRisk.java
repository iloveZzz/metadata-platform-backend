package com.yss.metadata.application.lineage.service.support;

/**
 * 影响风险等级推导（sortBy=risk 排序依据；受控解读，见切片报告）。
 *
 * <p>风险由资产分级分类推导：含「敏感」→ high；含「内部」→ medium；
 * 其余（含空）→ low。不改变分类本身（分类传播为切片 05）。</p>
 */
public final class ImpactRisk {

    private ImpactRisk() {
    }

    /**
     * 分类 → 风险等级（high/medium/low）。
     */
    public static String of(String classification) {
        if (classification == null) {
            return "low";
        }
        String value = classification;
        if (value.contains("敏感")) {
            return "high";
        }
        if (value.contains("内部")) {
            return "medium";
        }
        return "low";
    }

    /**
     * 风险等级排序权重（sortBy=risk 降序用）。
     */
    public static int order(String risk) {
        if ("high".equals(risk)) {
            return 3;
        }
        if ("medium".equals(risk)) {
            return 2;
        }
        return 1;
    }
}
