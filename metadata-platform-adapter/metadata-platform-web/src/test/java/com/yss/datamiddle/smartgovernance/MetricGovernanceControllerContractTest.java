package com.yss.datamiddle.smartgovernance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.datamiddle.smartgovernance.application.service.MetricGovernanceApplicationService;
import com.yss.datamiddle.smartgovernance.domain.metric.model.ConflictStatus;
import com.yss.datamiddle.smartgovernance.domain.metric.model.ConflictType;
import com.yss.datamiddle.smartgovernance.domain.metric.model.MetricAstDiff;
import com.yss.datamiddle.smartgovernance.domain.metric.model.MetricConflictRecord;
import com.yss.datamiddle.smartgovernance.web.controller.MetricGovernanceController;
import com.yss.datamiddle.smartgovernance.web.dto.ReconcileMetricConflictDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MetricGovernanceController.class)
@ContextConfiguration(classes = {MetricGovernanceController.class})
class MetricGovernanceControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetricGovernanceApplicationService metricService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GET /api/smart-governance/metrics/conflicts 返回冲突列表分页")
    void testListConflicts() throws Exception {
        MetricConflictRecord record = MetricConflictRecord.builder()
                .id("mc-001")
                .conflictCode("MC_20260817_01")
                .indicatorAName("全渠道GMV")
                .indicatorBName("平台成交额")
                .conflictType(ConflictType.FORMULA_DRIFT)
                .similarityScore(new BigDecimal("0.88"))
                .status(ConflictStatus.UNRESOLVED)
                .createdAt(LocalDateTime.now())
                .build();

        when(metricService.queryConflicts(any(), any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(record));
        when(metricService.countConflicts(any(), any(), any())).thenReturn(1L);

        mockMvc.perform(get("/api/smart-governance/metrics/conflicts?pageIndex=1&pageSize=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.data[0].indicatorAName").value("全渠道GMV"))
                .andExpect(jsonPath("$.data[0].conflictType").value("FORMULA_DRIFT"));
    }

    @Test
    @DisplayName("GET /api/smart-governance/metrics/conflicts/{id}/diff 返回 Side-by-Side 差异数据")
    void testGetConflictDiff() throws Exception {
        Map<String, Object> diffMap = new HashMap<>();
        diffMap.put("astDiff", MetricAstDiff.builder()
                .conflictType(ConflictType.FORMULA_DRIFT)
                .similarityScore(0.86)
                .aggMatch(true)
                .whereClauseDiff("状态值差异: status in (1, 2) vs status = 1")
                .build());

        when(metricService.getConflictDiff("mc-001")).thenReturn(diffMap);

        mockMvc.perform(get("/api/smart-governance/metrics/conflicts/mc-001/diff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.astDiff.aggMatch").value(true));
    }

    @Test
    @DisplayName("POST /api/smart-governance/metrics/conflicts/{id}/reconcile 执行一键对齐归并")
    void testReconcile() throws Exception {
        ReconcileMetricConflictDTO dto = new ReconcileMetricConflictDTO();
        dto.setCanonicalIndicatorId("ind-001");
        dto.setReconcileStrategy("MERGE_TO_ALIAS");
        dto.setComment("归并为财务标准指标");

        mockMvc.perform(post("/api/smart-governance/metrics/conflicts/mc-001/reconcile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }
}
