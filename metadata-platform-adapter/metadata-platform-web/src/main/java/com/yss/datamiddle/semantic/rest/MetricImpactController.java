package com.yss.datamiddle.semantic.rest;

import com.yss.cloud.dto.result.SingleResult;
import com.yss.datamiddle.semantic.application.service.MetricImpactAnalysisService;
import com.yss.datamiddle.semantic.client.vo.ImpactedEntityVO;
import com.yss.datamiddle.semantic.client.vo.MetricImpactAnalysisVO;
import com.yss.datamiddle.semantic.metric.impact.MetricImpactAnalysisResult;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 指标口径变更级联影响分析 REST 控制器 (SL-P1-03)
 */
@RestController
@RequestMapping("/api/semantic/metrics")
@RequiredArgsConstructor
public class MetricImpactController {

    private final MetricImpactAnalysisService impactAnalysisService;

    @GetMapping("/{id}/impact-analysis")
    public SingleResult<MetricImpactAnalysisVO> analyzeImpact(@PathVariable("id") Long id) {
        MetricImpactAnalysisResult result = impactAnalysisService.analyzeImpact(id);

        List<ImpactedEntityVO> entities = Collections.emptyList();
        if (result.getImpactedEntities() != null) {
            entities = result.getImpactedEntities().stream().map(e -> ImpactedEntityVO.builder()
                    .entityId(e.getEntityId())
                    .entityType(e.getEntityType())
                    .entityName(e.getEntityName())
                    .owner(e.getOwner())
                    .impactDescription(e.getImpactDescription())
                    .build()).collect(Collectors.toList());
        }

        MetricImpactAnalysisVO vo = MetricImpactAnalysisVO.builder()
                .metricId(result.getMetricId())
                .metricName(result.getMetricName())
                .impactLevel(result.getImpactLevel())
                .downstreamMetricCount(result.getDownstreamMetricCount())
                .associatedAssetCount(result.getAssociatedAssetCount())
                .impactedEntities(entities)
                .recommendations(result.getRecommendations())
                .build();

        return SingleResult.of(vo);
    }
}
