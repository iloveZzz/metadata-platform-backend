package com.yss.datamiddle.semantic.metric.impact;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 指标口径变更级联影响分析结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricImpactAnalysisResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long metricId;
    private String metricName;
    private ImpactLevel impactLevel;
    private int downstreamMetricCount;
    private int associatedAssetCount;
    @Builder.Default
    private List<ImpactedEntity> impactedEntities = new ArrayList<>();
    @Builder.Default
    private List<String> recommendations = new ArrayList<>();
}
