package com.yss.datamiddle.semantic.rest;

import com.yss.datamiddle.semantic.application.service.MetricImpactAnalysisService;
import com.yss.datamiddle.semantic.metric.impact.ImpactLevel;
import com.yss.datamiddle.semantic.metric.impact.MetricImpactAnalysisResult;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MetricImpactControllerContractTest {

    private MockMvc mockMvc;
    private MetricImpactAnalysisService impactAnalysisService;

    @BeforeEach
    public void setUp() {
        impactAnalysisService = Mockito.mock(MetricImpactAnalysisService.class);
        MetricImpactController controller = new MetricImpactController(impactAnalysisService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testAnalyzeImpactContract() throws Exception {
        MetricImpactAnalysisResult result = MetricImpactAnalysisResult.builder()
                .metricId(1L)
                .metricName("GMV")
                .impactLevel(ImpactLevel.HIGH)
                .downstreamMetricCount(2)
                .associatedAssetCount(3)
                .impactedEntities(Collections.emptyList())
                .recommendations(Collections.singletonList("建议保留历史版本快照"))
                .build();

        Mockito.when(impactAnalysisService.analyzeImpact(1L)).thenReturn(result);

        mockMvc.perform(get("/api/semantic/metrics/1/impact-analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DM-A0001"))
                .andExpect(jsonPath("$.data.metricId").value(1))
                .andExpect(jsonPath("$.data.metricName").value("GMV"))
                .andExpect(jsonPath("$.data.impactLevel").value("HIGH"))
                .andExpect(jsonPath("$.data.downstreamMetricCount").value(2));
    }
}
