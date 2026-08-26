package com.yss.datamiddle.semantic.metric.impact;

import com.yss.datamiddle.semantic.attachment.model.Attachment;
import com.yss.datamiddle.semantic.attachment.model.AttachmentStatus;
import com.yss.datamiddle.semantic.attachment.model.SemanticObjectType;
import com.yss.datamiddle.semantic.metric.model.MetricDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MetricImpactAnalyzerTest {

    private final MetricImpactAnalyzer analyzer = new MetricImpactAnalyzer();

    @Test
    public void testHighImpactForAuthoritativeMetric() {
        MetricDefinition m1 = MetricDefinition.create("营业收入", "财务域", "总营收", "cfo", "admin");
        m1.setId(1L);
        m1.setAuthoritative(true);

        MetricDefinition m2 = MetricDefinition.create("净利润", "财务域", "净利润口径", "cfo", "admin");
        m2.setId(2L);
        m2.addVersion("营业收入 - 总成本", "衍生指标公式", Collections.emptyList(), null, "admin");

        List<Attachment> attachments = Collections.singletonList(
                Attachment.create(101L, com.yss.datamiddle.semantic.attachment.model.AttachmentLevel.COLUMN, "amount", SemanticObjectType.METRIC, 1L, "admin")
        );

        MetricImpactAnalysisResult result = analyzer.analyze(m1, Collections.singletonList(m2), attachments);

        Assertions.assertEquals(ImpactLevel.HIGH, result.getImpactLevel());
        Assertions.assertEquals(1, result.getDownstreamMetricCount());
        Assertions.assertEquals(1, result.getAssociatedAssetCount());
        Assertions.assertEquals(2, result.getImpactedEntities().size());
        Assertions.assertTrue(result.getRecommendations().get(0).contains("高风险"));
    }

    @Test
    public void testLowImpactForIsolatedMetric() {
        MetricDefinition m1 = MetricDefinition.create("临时测试指标", "测试", "无依赖", "dev", "admin");
        m1.setId(99L);
        m1.setAuthoritative(false);

        MetricImpactAnalysisResult result = analyzer.analyze(m1, Collections.emptyList(), Collections.emptyList());

        Assertions.assertEquals(ImpactLevel.LOW, result.getImpactLevel());
        Assertions.assertEquals(0, result.getDownstreamMetricCount());
        Assertions.assertEquals(0, result.getAssociatedAssetCount());
    }
}
