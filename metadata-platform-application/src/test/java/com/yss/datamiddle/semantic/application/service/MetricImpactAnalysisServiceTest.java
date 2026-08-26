package com.yss.datamiddle.semantic.application.service;

import com.yss.datamiddle.semantic.attachment.gateway.AttachmentGateway;
import com.yss.datamiddle.semantic.attachment.model.Attachment;
import com.yss.datamiddle.semantic.attachment.model.AttachmentLevel;
import com.yss.datamiddle.semantic.attachment.model.AttachmentStatus;
import com.yss.datamiddle.semantic.attachment.model.SemanticObjectType;
import com.yss.datamiddle.semantic.metric.gateway.MetricGateway;
import com.yss.datamiddle.semantic.metric.impact.ImpactLevel;
import com.yss.datamiddle.semantic.metric.impact.MetricImpactAnalysisResult;
import com.yss.datamiddle.semantic.metric.model.MetricDefinition;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class MetricImpactAnalysisServiceTest {

    private MetricGateway metricGateway;
    private AttachmentGateway attachmentGateway;
    private MetricImpactAnalysisService service;

    @BeforeEach
    public void setUp() {
        metricGateway = Mockito.mock(MetricGateway.class);
        attachmentGateway = Mockito.mock(AttachmentGateway.class);
        service = new MetricImpactAnalysisService(metricGateway, attachmentGateway);
    }

    @Test
    public void testAnalyzeImpactSuccess() {
        MetricDefinition m1 = MetricDefinition.create("GMV", "交易域", "成交总额", "biz", "admin");
        m1.setId(1L);
        m1.setAuthoritative(true);

        Mockito.when(metricGateway.findById(1L)).thenReturn(Optional.of(m1));
        Mockito.when(metricGateway.listAll()).thenReturn(Collections.singletonList(m1));

        Attachment a = Attachment.create(200L, AttachmentLevel.COLUMN, "gmv_amt", SemanticObjectType.METRIC, 1L, "admin");
        Mockito.when(attachmentGateway.query(null, null, SemanticObjectType.METRIC, 1L, AttachmentStatus.ACTIVE))
                .thenReturn(Collections.singletonList(a));

        MetricImpactAnalysisResult result = service.analyzeImpact(1L);

        Assertions.assertEquals(1L, result.getMetricId());
        Assertions.assertEquals("GMV", result.getMetricName());
        Assertions.assertEquals(ImpactLevel.HIGH, result.getImpactLevel());
        Assertions.assertEquals(1, result.getAssociatedAssetCount());
    }

    @Test
    public void testAnalyzeNonExistentMetricThrows() {
        Mockito.when(metricGateway.findById(999L)).thenReturn(Optional.empty());

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            service.analyzeImpact(999L);
        });
    }
}
