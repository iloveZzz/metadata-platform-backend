package com.yss.datamiddle.semantic.metric.impact;

import com.yss.datamiddle.semantic.attachment.model.Attachment;
import com.yss.datamiddle.semantic.metric.model.MetricDefinition;
import com.yss.datamiddle.semantic.metric.model.MetricVersion;
import java.util.ArrayList;
import java.util.List;

/**
 * 指标级联影响分析领域算法引擎
 */
public class MetricImpactAnalyzer {

    /**
     * 分析目标指标变更的影响面
     *
     * @param targetMetric 目标指标
     * @param allMetrics   系统中全部指标（用于查找下游引用）
     * @param attachments  目标指标当前挂接的有效资产
     * @return 影响分析报告
     */
    public MetricImpactAnalysisResult analyze(
            MetricDefinition targetMetric,
            List<MetricDefinition> allMetrics,
            List<Attachment> attachments
    ) {
        if (targetMetric == null) {
            throw new IllegalArgumentException("targetMetric cannot be null");
        }

        List<ImpactedEntity> impactedEntities = new ArrayList<>();
        int downstreamMetricCount = 0;

        // 1. 查找依赖该指标的下游指标（例如复合指标在计算公式中包含 targetMetric.name）
        if (allMetrics != null) {
            String targetName = targetMetric.getName();
            for (MetricDefinition m : allMetrics) {
                if (m.getId() != null && m.getId().equals(targetMetric.getId())) {
                    continue;
                }
                boolean depends = false;
                if (m.getVersions() != null) {
                    for (MetricVersion v : m.getVersions()) {
                        if (v.getExpression() != null && v.getExpression().contains(targetName)) {
                            depends = true;
                            break;
                        }
                    }
                }
                if (depends) {
                    downstreamMetricCount++;
                    impactedEntities.add(ImpactedEntity.builder()
                            .entityId(m.getId())
                            .entityType("METRIC")
                            .entityName(m.getName())
                            .owner(m.getOwner())
                            .impactDescription("下游衍生指标公式依赖本指标: " + targetName)
                            .build());
                }
            }
        }

        // 2. 统计挂接的元数据资产
        int associatedAssetCount = 0;
        if (attachments != null) {
            for (Attachment a : attachments) {
                associatedAssetCount++;
                impactedEntities.add(ImpactedEntity.builder()
                        .entityId(a.getAssetId())
                        .entityType("ASSET")
                        .entityName("资产ID#" + a.getAssetId() + (a.getColumnName() != null ? " (" + a.getColumnName() + ")" : ""))
                        .owner(a.getCreatedBy())
                        .impactDescription("数据资产字段语义绑定")
                        .build());
            }
        }

        // 3. 计算综合影响等级 (HIGH / MEDIUM / LOW)
        ImpactLevel level;
        List<String> recommendations = new ArrayList<>();

        if (Boolean.TRUE.equals(targetMetric.getAuthoritative()) || downstreamMetricCount >= 3 || associatedAssetCount >= 5) {
            level = ImpactLevel.HIGH;
            recommendations.add("【高风险】该指标为权威认证指标或存在大量下游依赖，口径变更前必须发起审批流并通知所有下游负责人");
            recommendations.add("建议在修改口径前创建新的指标版本快照，并保留历史版本回滚点");
        } else if (downstreamMetricCount > 0 || associatedAssetCount > 0) {
            level = ImpactLevel.MEDIUM;
            recommendations.add("【中风险】存在关联资产或下游指标，建议变更后同步通知相关负责人");
        } else {
            level = ImpactLevel.LOW;
            recommendations.add("【低风险】当前无外部强依赖，可直接更新口径版本");
        }

        return MetricImpactAnalysisResult.builder()
                .metricId(targetMetric.getId())
                .metricName(targetMetric.getName())
                .impactLevel(level)
                .downstreamMetricCount(downstreamMetricCount)
                .associatedAssetCount(associatedAssetCount)
                .impactedEntities(impactedEntities)
                .recommendations(recommendations)
                .build();
    }
}
