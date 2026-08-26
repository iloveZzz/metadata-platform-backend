package com.yss.metadata;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ReadOnlyToolRegistry;
import com.yss.datamiddle.aicontextlayer.mcpserver.tool.McpToolDispatcher;
import com.yss.metadata.application.lineage.service.ColumnImpactAnalysisService;
import com.yss.metadata.application.lineage.service.ColumnLineageAppService;
import com.yss.metadata.client.dto.cmd.ColumnLineageManualCmd;
import com.yss.metadata.client.vo.ColumnImpactAnalysisVO;
import com.yss.metadata.client.vo.ColumnLineageGraphVO;
import com.yss.metadata.domain.asset.gateway.AssetRepository;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetColumn;
import com.yss.metadata.domain.asset.model.AssetStatus;
import com.yss.metadata.repository.AssetColumnRepository;
import com.yss.metadata.repository.entity.AssetColumnPO;
import com.yss.metadata.domain.lineage.exception.LineageCycleException;
import com.yss.metadata.domain.lineage.gateway.LineageGraphRepository;
import com.yss.metadata.domain.lineage.model.LineageConfidence;
import com.yss.metadata.domain.lineage.model.LineageEdge;
import com.yss.metadata.domain.lineage.model.LineageType;
import com.yss.metadata.domain.lineage.parser.SqlLineageParser;
import com.yss.metadata.domain.lineage.parser.model.ColumnLineage;
import com.yss.metadata.domain.lineage.parser.model.SqlLineageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 字段级血缘引擎、爆炸半径 BFS 算法与 MCP 工具集成端到端 (E2E) 测试。
 */
@SpringBootTest(classes = MetadataPlatformApplication.class, properties = {
        "spring.datasource.primary.url=jdbc:h2:mem:column_lineage_e2e;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.primary.driver-class-name=org.h2.Driver",
        "spring.datasource.primary.username=sa",
        "spring.datasource.primary.password=",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "spring.liquibase.enabled=false"
})
class ColumnLevelLineageAndImpactE2ETest {

    @Autowired
    private SqlLineageParser sqlLineageParser;

    @Autowired
    private LineageGraphRepository lineageGraphRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private ColumnLineageAppService columnLineageAppService;

    @Autowired
    private ColumnImpactAnalysisService columnImpactAnalysisService;

    @Autowired(required = false)
    private McpToolDispatcher mcpToolDispatcher;

    @Autowired
    private AssetColumnRepository assetColumnRepository;

    private String odsAssetId;
    private String dwdAssetId;
    private String adsAssetId;

    @BeforeEach
    void setUp() {
        // 1. 初始化数仓三层资产 (ODS -> DWD -> ADS)
        odsAssetId = "ast-ods-trade";
        Asset ods = Asset.builder()
                .id(odsAssetId)
                .sourceId("src-test-01")
                .name("ods_trade_order")
                .type("table")
                .status(AssetStatus.CLAIMED)
                .build();
        assetRepository.save(ods);

        assetColumnRepository.insert(AssetColumnPO.builder().id("col-ods-id").assetId(odsAssetId).name("order_id").type("BIGINT").pk(true).build());
        assetColumnRepository.insert(AssetColumnPO.builder().id("col-ods-amt").assetId(odsAssetId).name("amount").type("DECIMAL(18,2)").classification("S2").build());
        assetColumnRepository.insert(AssetColumnPO.builder().id("col-ods-user").assetId(odsAssetId).name("user_id").type("VARCHAR(64)").classification("S3").build());

        dwdAssetId = "ast-dwd-trade";
        Asset dwd = Asset.builder()
                .id(dwdAssetId)
                .sourceId("src-test-01")
                .name("dwd_trade_order_di")
                .type("table")
                .status(AssetStatus.CLAIMED)
                .build();
        assetRepository.save(dwd);

        assetColumnRepository.insert(AssetColumnPO.builder().id("col-dwd-id").assetId(dwdAssetId).name("trade_id").type("BIGINT").pk(true).build());
        assetColumnRepository.insert(AssetColumnPO.builder().id("col-dwd-amt").assetId(dwdAssetId).name("trade_amt").type("DECIMAL(18,2)").classification("S2").build());
        assetColumnRepository.insert(AssetColumnPO.builder().id("col-dwd-user").assetId(dwdAssetId).name("buyer_id").type("VARCHAR(64)").classification("S3").build());

        adsAssetId = "ast-ads-trade";
        Asset ads = Asset.builder()
                .id(adsAssetId)
                .sourceId("src-test-01")
                .name("ads_trade_stat")
                .type("table")
                .status(AssetStatus.CLAIMED)
                .build();
        assetRepository.save(ads);

        assetColumnRepository.insert(AssetColumnPO.builder().id("col-ads-amt").assetId(adsAssetId).name("total_trade_amt").type("DECIMAL(20,2)").classification("S4").build());
        assetColumnRepository.insert(AssetColumnPO.builder().id("col-ads-cnt").assetId(adsAssetId).name("trade_cnt").type("BIGINT").classification("S1").build());
    }

    @Test
    @DisplayName("E2E: AST 解析多层级数仓血缘 -> 字段图谱检索 -> 下游爆炸半径 BFS 计算 -> MCP 工具调度 -> 闭环防御")
    void testEndToEndColumnLineageWorkflow() {
        // Step 1: SQL AST 语法解析验证 (ODS -> DWD)
        String dwdSql = "CREATE VIEW dwd_trade_order_di AS " +
                "SELECT order_id AS trade_id, amount * 0.9 AS trade_amt, user_id AS buyer_id FROM ods_trade_order";
        SqlLineageResult dwdResult = sqlLineageParser.parse(dwdSql);
        assertThat(dwdResult.isSupported()).isTrue();
        assertThat(dwdResult.getColumnLineage()).hasSize(3);

        // Step 2: 保存 ODS -> DWD 字段级血缘
        for (ColumnLineage col : dwdResult.getColumnLineage()) {
            lineageGraphRepository.save(LineageEdge.builder()
                    .fromAssetId(odsAssetId)
                    .fromColumnId(col.getFromColumn())
                    .toAssetId(dwdAssetId)
                    .toColumnId(col.getToColumn())
                    .transformExpr(col.getTransformExpr())
                    .exprType(col.getExprType())
                    .type(LineageType.SQL)
                    .confidence(LineageConfidence.AUTO_HIGH)
                    .graphVersion("v1")
                    .build());
        }

        // Step 3: 保存 DWD -> ADS 聚合字段血缘 (trade_amt -> total_trade_amt)
        lineageGraphRepository.save(LineageEdge.builder()
                .fromAssetId(dwdAssetId)
                .fromColumnId("trade_amt")
                .toAssetId(adsAssetId)
                .toColumnId("total_trade_amt")
                .transformExpr("sum(trade_amt)")
                .exprType("AGGREGATE")
                .type(LineageType.SQL)
                .confidence(LineageConfidence.AUTO_HIGH)
                .graphVersion("v2")
                .build());

        // Step 4: 检索 DWD 节点的字段级血缘图谱
        ColumnLineageGraphVO graphVO = columnLineageAppService.getColumnLineageGraph(dwdAssetId, null, 3, "BOTH");
        assertThat(graphVO).isNotNull();
        assertThat(graphVO.getNodes()).isNotEmpty();
        assertThat(graphVO.getEdges()).hasSize(4);

        // Step 5: 计算 ODS 表 amount 字段的下游爆炸半径 (Blast Radius)
        ColumnImpactAnalysisVO impact = columnImpactAnalysisService.analyzeImpact(odsAssetId, "amount", 5);
        assertThat(impact).isNotNull();
        assertThat(impact.getSourceAssetName()).isEqualTo("ods_trade_order");
        assertThat(impact.getImpactSummary().getTotalAffectedAssets()).isEqualTo(2); // DWD + ADS
        assertThat(impact.getImpactSummary().getTotalAffectedColumns()).isEqualTo(2); // trade_amt + total_trade_amt
        assertThat(impact.getImpactSummary().getMaxDepth()).isEqualTo(2);
        assertThat(impact.getImpactSummary().getHasCriticalDownstream()).isTrue(); // S4 资产被波及
        assertThat(impact.getImpactLayers()).hasSize(2);
        assertThat(impact.getImpactLayers().get(0).getDepth()).isEqualTo(1);
        assertThat(impact.getImpactLayers().get(0).getAffectedColumns().get(0).getColumnName()).isEqualTo("trade_amt");
        assertThat(impact.getImpactLayers().get(1).getDepth()).isEqualTo(2);
        assertThat(impact.getImpactLayers().get(1).getAffectedColumns().get(0).getColumnName()).isEqualTo("total_trade_amt");

        // Step 6: MCP 协议调用 column_impact_analysis 工具分发
        if (mcpToolDispatcher != null) {
            Map<String, Object> mcpArgs = new HashMap<>();
            mcpArgs.put("asset_id", odsAssetId);
            mcpArgs.put("column_id", "amount");
            mcpArgs.put("max_depth", 5);

            Object mcpResult = mcpToolDispatcher.dispatch("test-agent", "http://localhost",
                    ReadOnlyToolRegistry.TOOL_COLUMN_IMPACT_ANALYSIS, mcpArgs);
            assertThat(mcpResult).isInstanceOf(ColumnImpactAnalysisVO.class);
            ColumnImpactAnalysisVO mcpImpact = (ColumnImpactAnalysisVO) mcpResult;
            assertThat(mcpImpact.getImpactSummary().getTotalAffectedAssets()).isEqualTo(2);
        }

        // Step 7: 闭环防御：尝试从 ADS.total_trade_amt 补录回 ODS.amount
        ColumnLineageManualCmd cycleCmd = ColumnLineageManualCmd.builder()
                .fromAssetId(adsAssetId)
                .fromColumnId("total_trade_amt")
                .toAssetId(odsAssetId)
                .toColumnId("amount")
                .build();

        assertThatThrownBy(() -> columnLineageAppService.addManualColumnEdge(cycleCmd, "test-user"))
                .isInstanceOf(LineageCycleException.class);
    }
}
