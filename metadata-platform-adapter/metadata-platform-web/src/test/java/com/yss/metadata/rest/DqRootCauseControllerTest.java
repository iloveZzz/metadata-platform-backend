package com.yss.metadata.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.metadata.application.dq.BlastRadiusApplicationService;
import com.yss.metadata.application.dq.DqRootCauseApplicationService;
import com.yss.metadata.domain.dq.gateway.BlastRadiusGateway;
import com.yss.metadata.domain.dq.gateway.RootCauseAnalysisGateway;
import com.yss.metadata.domain.dq.model.BlastRadiusAsset;
import com.yss.metadata.domain.dq.model.BlastRadiusReport;
import com.yss.metadata.domain.dq.model.PropagationStep;
import com.yss.metadata.domain.dq.model.RootCauseNode;
import com.yss.metadata.domain.dq.model.RootCauseReport;
import com.yss.metadata.application.dq.convertor.DqObservabilityConvertor;
import com.yss.metadata.rest.advice.MetadataGlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 质量可观测性与根因/爆炸半径控制器契约测试
 *
 * @author ai
 * @since 2026-08-15
 */
class DqRootCauseControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RootCauseAnalysisGateway rootCauseGateway = targetAssetId -> {
            RootCauseNode root = RootCauseNode.builder()
                    .assetId("ast-ods-01")
                    .assetName("ods_trade_log")
                    .domain("trade")
                    .healthScore(45)
                    .qualityBand("poor")
                    .taintStatus("TAINTED")
                    .ruleName("核心唯一键 duplicate_count 越界")
                    .actualMetric("重复记录 3,420 条")
                    .threshold("duplicate_count == 0")
                    .faultTime("2026-08-15 10:00:00")
                    .distance(2)
                    .build();

            PropagationStep step = PropagationStep.builder()
                    .fromAssetId("ast-ods-01")
                    .fromAssetName("ods_trade_log")
                    .toAssetId(targetAssetId)
                    .toAssetName("dwd_trade_order_di")
                    .propagationType("SQL ETL 派生污染")
                    .build();

            return RootCauseReport.builder()
                    .targetAssetId(targetAssetId)
                    .rootAsset(root)
                    .propagationPath(Collections.singletonList(step))
                    .confidence("94%")
                    .summary("上游根因定位为 ods_trade_log")
                    .suggestions(Arrays.asList("通知负责人补数", "标记全链路存疑"))
                    .createdAt(LocalDateTime.now())
                    .build();
        };

        BlastRadiusGateway blastRadiusGateway = (originAssetId, maxDepth) -> {
            BlastRadiusAsset downstream1 = BlastRadiusAsset.builder()
                    .assetId("ast-dws-01")
                    .assetName("dws_trade_summary_di")
                    .domain("trade")
                    .depth(1)
                    .owner("trade-owner")
                    .healthScore(65)
                    .qualityBand("fair")
                    .taintStatus("NORMAL")
                    .build();

            return BlastRadiusReport.builder()
                    .originAssetId(originAssetId)
                    .originAssetName("dwd_trade_order_di")
                    .impactedAssets(Collections.singletonList(downstream1))
                    .totalImpactedCount(1)
                    .maxDepth(1)
                    .impactedDomains(Collections.singletonList("trade"))
                    .build();
        };

        DqRootCauseApplicationService rootCauseService = new DqRootCauseApplicationService(rootCauseGateway);
        BlastRadiusApplicationService blastRadiusService = new BlastRadiusApplicationService(blastRadiusGateway);

        DqRootCauseController controller = new DqRootCauseController(
                rootCauseService, blastRadiusService, Mappers.getMapper(DqObservabilityConvertor.class));

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new MetadataGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/dq/assets/{id}/root-cause 返回 200 与 RootCause 报表")
    void testGetRootCauseSuccess() throws Exception {
        mockMvc.perform(get("/api/dq/assets/ast-dwd-01/root-cause")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("DM-A0001"))
                .andExpect(jsonPath("$.data.targetAssetId").value("ast-dwd-01"))
                .andExpect(jsonPath("$.data.rootAsset.assetName").value("ods_trade_log"))
                .andExpect(jsonPath("$.data.rootAsset.healthScore").value(45))
                .andExpect(jsonPath("$.data.confidence").value("94%"))
                .andExpect(jsonPath("$.data.propagationPath", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/dq/assets/{id}/blast-radius 返回 200 与 BlastRadius 报表")
    void testGetBlastRadiusSuccess() throws Exception {
        mockMvc.perform(get("/api/dq/assets/ast-dwd-01/blast-radius")
                        .param("maxDepth", "3")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("DM-A0001"))
                .andExpect(jsonPath("$.data.originAssetId").value("ast-dwd-01"))
                .andExpect(jsonPath("$.data.totalImpactedCount").value(1))
                .andExpect(jsonPath("$.data.impactedAssets", hasSize(1)))
                .andExpect(jsonPath("$.data.impactedAssets[0].assetName").value("dws_trade_summary_di"));
    }
}
