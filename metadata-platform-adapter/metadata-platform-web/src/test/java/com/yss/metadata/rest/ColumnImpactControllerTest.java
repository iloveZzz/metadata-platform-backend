package com.yss.metadata.rest;

import com.yss.metadata.application.asset.support.InMemoryAssetRepository;
import com.yss.metadata.application.lineage.service.ColumnImpactAnalysisService;
import com.yss.metadata.application.lineage.service.impl.ColumnImpactAnalysisServiceImpl;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetColumn;
import com.yss.metadata.domain.asset.model.AssetStatus;
import com.yss.metadata.domain.lineage.model.LineageConfidence;
import com.yss.metadata.domain.lineage.model.LineageEdge;
import com.yss.metadata.domain.lineage.model.LineageType;
import com.yss.metadata.rest.advice.MetadataGlobalExceptionHandler;
import com.yss.metadata.rest.support.InMemoryLineageGraphRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 字段级下游爆炸半径影响分析 REST 契约测试。
 */
class ColumnImpactControllerTest {

    private InMemoryAssetRepository assetRepository;
    private InMemoryLineageGraphRepository graphRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        assetRepository = new InMemoryAssetRepository();
        graphRepository = new InMemoryLineageGraphRepository();

        ColumnImpactAnalysisService impactService = new ColumnImpactAnalysisServiceImpl(
                graphRepository,
                assetRepository
        );

        mockMvc = MockMvcBuilders.standaloneSetup(new ColumnImpactController(impactService))
                .setControllerAdvice(new MetadataGlobalExceptionHandler())
                .build();

        seedAssets();
    }

    private void seedAssets() {
        assetRepository.seedSourceName("s-1", "测试库");

        Asset ods = Asset.builder().id("ast-ods").sourceId("s-1").name("ods_user").status(AssetStatus.CLAIMED).build();
        assetRepository.seed(ods);
        assetRepository.seedColumns("ast-ods", Collections.singletonList(
                AssetColumn.builder().id("col-phone").name("phone").type("VARCHAR(32)").classification("S3").build()
        ));

        Asset dwd = Asset.builder().id("ast-dwd").sourceId("s-1").name("dwd_user").status(AssetStatus.CLAIMED).build();
        assetRepository.seed(dwd);
        assetRepository.seedColumns("ast-dwd", Collections.singletonList(
                AssetColumn.builder().id("col-masked-phone").name("masked_phone").type("VARCHAR(32)").classification("S3").build()
        ));

        Asset ads = Asset.builder().id("ast-ads").sourceId("s-1").name("ads_user_stat").status(AssetStatus.CLAIMED).build();
        assetRepository.seed(ads);
        assetRepository.seedColumns("ast-ads", Collections.singletonList(
                AssetColumn.builder().id("col-user-cnt").name("user_cnt").type("BIGINT").classification("S1").build()
        ));
    }

    @Test
    @DisplayName("GET 字段下游爆炸半径分析：层级扩散与高危敏感资产识别")
    void testAnalyzeImpactSpreadAndCriticalDetection() throws Exception {
        // ods.phone -> dwd.masked_phone (depth 1)
        graphRepository.seed(LineageEdge.builder()
                .id("e1")
                .fromAssetId("ast-ods")
                .fromColumnId("phone")
                .toAssetId("ast-dwd")
                .toColumnId("masked_phone")
                .transformExpr("mask(phone)")
                .exprType("COMPUTED")
                .type(LineageType.SQL)
                .confidence(LineageConfidence.AUTO_HIGH)
                .build());

        // dwd.masked_phone -> ads.user_cnt (depth 2)
        graphRepository.seed(LineageEdge.builder()
                .id("e2")
                .fromAssetId("ast-dwd")
                .fromColumnId("masked_phone")
                .toAssetId("ast-ads")
                .toColumnId("user_cnt")
                .transformExpr("count(masked_phone)")
                .exprType("AGGREGATE")
                .type(LineageType.SQL)
                .confidence(LineageConfidence.AUTO_HIGH)
                .build());

        mockMvc.perform(get("/api/assets/ast-ods/columns/phone/impact-analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceAssetId", is("ast-ods")))
                .andExpect(jsonPath("$.data.sourceColumnName", is("phone")))
                .andExpect(jsonPath("$.data.impactSummary.totalAffectedAssets", is(2)))
                .andExpect(jsonPath("$.data.impactSummary.totalAffectedColumns", is(2)))
                .andExpect(jsonPath("$.data.impactSummary.maxDepth", is(2)))
                .andExpect(jsonPath("$.data.impactSummary.hasCriticalDownstream", is(true)))
                .andExpect(jsonPath("$.data.impactLayers", hasSize(2)))
                .andExpect(jsonPath("$.data.impactLayers[0].depth", is(1)))
                .andExpect(jsonPath("$.data.impactLayers[0].affectedColumns[0].columnName", is("masked_phone")))
                .andExpect(jsonPath("$.data.impactLayers[1].depth", is(2)))
                .andExpect(jsonPath("$.data.impactLayers[1].affectedColumns[0].columnName", is("user_cnt")));
    }
}
