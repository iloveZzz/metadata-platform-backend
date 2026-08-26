package com.yss.datamiddle.semantic.application.service;

import com.yss.datamiddle.semantic.attachment.gateway.AttachmentGateway;
import com.yss.datamiddle.semantic.attachment.model.Attachment;
import com.yss.datamiddle.semantic.attachment.model.AttachmentStatus;
import com.yss.datamiddle.semantic.attachment.model.SemanticObjectType;
import com.yss.datamiddle.semantic.metric.gateway.MetricGateway;
import com.yss.datamiddle.semantic.metric.impact.MetricImpactAnalysisResult;
import com.yss.datamiddle.semantic.metric.impact.MetricImpactAnalyzer;
import com.yss.datamiddle.semantic.metric.model.MetricDefinition;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 指标口径变更级联影响分析应用服务
 */
@Service
public class MetricImpactAnalysisService {

    private final MetricGateway metricGateway;
    private final AttachmentGateway attachmentGateway;
    private final MetricImpactAnalyzer impactAnalyzer = new MetricImpactAnalyzer();

    public MetricImpactAnalysisService(
            MetricGateway metricGateway,
            AttachmentGateway attachmentGateway
    ) {
        this.metricGateway = metricGateway;
        this.attachmentGateway = attachmentGateway;
    }

    /**
     * 分析指定指标的影响面
     */
    @Transactional(readOnly = true)
    public MetricImpactAnalysisResult analyzeImpact(Long metricId) {
        MetricDefinition targetMetric = metricGateway.findById(metricId)
                .orElseThrow(() -> new IllegalArgumentException("指标不存在: " + metricId));

        List<MetricDefinition> allMetrics = metricGateway.listAll();
        List<Attachment> activeAttachments = attachmentGateway.query(
                null,
                null,
                SemanticObjectType.METRIC,
                metricId,
                AttachmentStatus.ACTIVE
        );

        return impactAnalyzer.analyze(targetMetric, allMetrics, activeAttachments);
    }
}
