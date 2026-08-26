package com.yss.metadata.application.dq;

import com.yss.metadata.domain.dq.gateway.BlastRadiusGateway;
import com.yss.metadata.domain.dq.gateway.RootCauseAnalysisGateway;
import com.yss.metadata.domain.dq.gateway.TaintStatusGateway;
import com.yss.metadata.domain.dq.model.BlastRadiusAsset;
import com.yss.metadata.domain.dq.model.BlastRadiusReport;
import com.yss.metadata.domain.dq.model.PropagationStep;
import com.yss.metadata.domain.dq.model.RootCauseNode;
import com.yss.metadata.domain.dq.model.RootCauseReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 质量根因溯源与爆炸半径应用用例测试
 *
 * @author ai
 * @since 2026-08-15
 */
class DqRootCauseServiceTest {

    private DqRootCauseApplicationService rootCauseService;
    private BlastRadiusApplicationService blastRadiusService;
    private TaintStatusApplicationService taintStatusService;

    private List<String> updatedTaintLogs;

    @BeforeEach
    void setUp() {
        updatedTaintLogs = new ArrayList<>();

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

        TaintStatusGateway taintStatusGateway = (assetId, taintStatus, reason, operator) -> {
            updatedTaintLogs.add(assetId + ":" + taintStatus + ":" + operator);
        };

        rootCauseService = new DqRootCauseApplicationService(rootCauseGateway);
        blastRadiusService = new BlastRadiusApplicationService(blastRadiusGateway);
        taintStatusService = new TaintStatusApplicationService(taintStatusGateway);
    }

    @Test
    @DisplayName("根因溯源：定位最上游故障节点与传播链")
    void testRootCauseAnalysis() {
        RootCauseReport report = rootCauseService.analyzeRootCause("ast-dwd-01");

        assertThat(report).isNotNull();
        assertThat(report.getRootAsset().getAssetName()).isEqualTo("ods_trade_log");
        assertThat(report.getRootAsset().getRuleName()).contains("duplicate_count");
        assertThat(report.getConfidence()).isEqualTo("94%");
        assertThat(report.getPropagationPath()).hasSize(1);
    }

    @Test
    @DisplayName("爆炸半径：向下递归召回受影响资产")
    void testBlastRadiusCalculation() {
        BlastRadiusReport report = blastRadiusService.calculateBlastRadius("ast-dwd-01", 3);

        assertThat(report).isNotNull();
        assertThat(report.getTotalImpactedCount()).isEqualTo(1);
        assertThat(report.getImpactedAssets().get(0).getAssetName()).isEqualTo("dws_trade_summary_di");
    }

    @Test
    @DisplayName("存疑流转：成功标记存疑并记录操作人")
    void testUpdateTaintStatus() {
        taintStatusService.updateTaintStatus("ast-dwd-01", "TAINTED", "上游根因污染", "test-user");

        assertThat(updatedTaintLogs).contains("ast-dwd-01:TAINTED:test-user");
    }
}
