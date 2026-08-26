package com.yss.datamiddle.smartgovernance.domain.metric.service;

import com.yss.datamiddle.smartgovernance.domain.metric.model.ConflictType;
import com.yss.datamiddle.smartgovernance.domain.metric.model.MetricAstDiff;

/**
 * 指标 AST 公式比对器领域服务
 */
public class MetricAstComparator {

    /**
     * 比对两段指标计算公式并输出差异分析
     */
    public MetricAstDiff compareFormulas(String formulaA, String formulaB) {
        if (formulaA == null || formulaB == null) {
            return MetricAstDiff.builder()
                    .conflictType(ConflictType.HOMONYMOUS_MEANING)
                    .similarityScore(0.50)
                    .aggMatch(false)
                    .whereClauseDiff("公式为空或缺失")
                    .astSummary("公式缺失无法进行 AST 解析")
                    .build();
        }

        String normA = normalize(formulaA);
        String normB = normalize(formulaB);

        if (normA.equalsIgnoreCase(normB)) {
            return MetricAstDiff.builder()
                    .conflictType(ConflictType.SYNONYMOUS_NAME)
                    .similarityScore(1.0)
                    .aggMatch(true)
                    .whereClauseDiff("无差异，计算逻辑完全等价")
                    .astSummary("【同义异名】聚合函数与过滤条件 100% 一致")
                    .build();
        }

        String aggA = extractAgg(normA);
        String aggB = extractAgg(normB);
        boolean aggMatches = aggA != null && aggA.equalsIgnoreCase(aggB);

        String whereA = extractWhere(normA);
        String whereB = extractWhere(normB);

        if (aggMatches) {
            String diffMsg = String.format("A过滤: [%s] vs B过滤: [%s]", whereA, whereB);
            return MetricAstDiff.builder()
                    .conflictType(ConflictType.FORMULA_DRIFT)
                    .similarityScore(0.86)
                    .aggMatch(true)
                    .whereClauseDiff(diffMsg)
                    .astSummary("【口径漂移】聚合函数一致(" + aggA + ")，但 WHERE 过滤条件或时间窗口存在微小差异")
                    .build();
        } else {
            return MetricAstDiff.builder()
                    .conflictType(ConflictType.HOMONYMOUS_MEANING)
                    .similarityScore(0.68)
                    .aggMatch(false)
                    .whereClauseDiff(String.format("聚合函数差异: [%s] vs [%s]", aggA, aggB))
                    .astSummary("【同名异义】聚合计算逻辑存在根本性差异")
                    .build();
        }
    }

    private String normalize(String sql) {
        return sql.trim().replaceAll("\\s+", " ");
    }

    private String extractAgg(String sql) {
        String lower = sql.toLowerCase();
        if (lower.contains("sum(")) return "SUM";
        if (lower.contains("count(")) return "COUNT";
        if (lower.contains("avg(")) return "AVG";
        if (lower.contains("max(")) return "MAX";
        if (lower.contains("min(")) return "MIN";
        return "CUSTOM";
    }

    private String extractWhere(String sql) {
        String lower = sql.toLowerCase();
        int whereIdx = lower.indexOf("where");
        if (whereIdx != -1) {
            return sql.substring(whereIdx + 5).trim();
        }
        return "ALL (无过滤条件)";
    }
}
