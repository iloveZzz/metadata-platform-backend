package com.yss.datamiddle.semantic.client.vo;

import com.yss.datamiddle.semantic.metric.impact.ImpactLevel;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricImpactAnalysisVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long metricId;
    private String metricName;
    private ImpactLevel impactLevel;
    private Integer downstreamMetricCount;
    private Integer associatedAssetCount;
    private List<ImpactedEntityVO> impactedEntities;
    private List<String> recommendations;
}
