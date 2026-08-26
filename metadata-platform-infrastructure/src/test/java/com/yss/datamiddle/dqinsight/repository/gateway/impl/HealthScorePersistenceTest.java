package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.yss.datamiddle.dqinsight.InfraTestApplication;
import com.yss.datamiddle.dqinsight.client.dto.query.HealthScorePageQuery;
import com.yss.datamiddle.dqinsight.client.vo.AssetHealthDetailVO;
import com.yss.datamiddle.dqinsight.client.vo.AssetHealthRowVO;
import com.yss.datamiddle.dqinsight.client.vo.FieldHealthVO;
import com.yss.datamiddle.dqinsight.client.vo.RuleDetailVO;
import com.yss.datamiddle.dqinsight.client.vo.RuleScoreVO;
import com.yss.datamiddle.dqinsight.domain.gateway.DqResultGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.HealthScoreGateway;
import com.yss.datamiddle.dqinsight.domain.model.BandFilter;
import com.yss.datamiddle.dqinsight.domain.model.DQResultBatch;
import com.yss.datamiddle.dqinsight.domain.model.FormatType;
import com.yss.datamiddle.dqinsight.domain.model.HealthBand;
import com.yss.datamiddle.dqinsight.domain.model.HealthScore;
import com.yss.datamiddle.dqinsight.domain.model.HealthState;
import com.yss.datamiddle.dqinsight.domain.model.RuleResultRow;
import com.yss.datamiddle.dqinsight.domain.model.RuleScoreSnapshot;
import com.yss.datamiddle.dqinsight.domain.model.RuleStatus;
import com.yss.datamiddle.dqinsight.domain.model.RuleType;
import com.yss.datamiddle.dqinsight.domain.model.SourceTool;
import com.yss.datamiddle.dqinsight.repository.DqHealthScoreRepository;
import com.yss.datamiddle.dqinsight.repository.entity.DqHealthScorePO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 健康分仓储集成测试（DQI-SLICE-02）：upsert 幂等 / 规则版本递增 / 规则明细权重快照 /
 * 列表筛选（档位 + 独立展示态）/ 详情字段级低分标记 / 钻取分数来源区。
 *
 * <p>测试库 H2（MySQL 模式），迁移脚本由 Liquibase 执行（同时验证建表脚本可执行）。</p>
 */
@SpringBootTest(classes = InfraTestApplication.class)
@Transactional
class HealthScorePersistenceTest {

    private static final Instant EXECUTION_TIME = Instant.parse("2026-08-11T10:00:00Z");

    @Autowired
    private HealthScoreGateway healthScoreGateway;

    @Autowired
    private DqResultGateway dqResultGateway;

    @Autowired
    private DqHealthScoreRepository dqHealthScoreRepository;

    @BeforeEach
    void setUp() {
        // 每用例独立批次号 / 资产 ID，避免跨用例干扰
    }

    @Test
    void upsertInsertsThenUpdatesSameAssetFieldRow() {
        HealthScore first = calculate("asset-up1", null, "v1", allPassedRows("asset-up1"));
        healthScoreGateway.upsert(first);

        List<DqHealthScorePO> afterFirst = dqHealthScoreRepository.selectList(null);
        assertThat(afterFirst).hasSize(1);
        assertThat(afterFirst.get(0).getRuleVersion()).isEqualTo("v1");

        HealthScore second = calculate("asset-up1", null, "v2", allPassedRows("asset-up1"));
        healthScoreGateway.upsert(second);

        List<DqHealthScorePO> afterSecond = dqHealthScoreRepository.selectList(null);
        assertThat(afterSecond).hasSize(1); // 每资产 / 字段保留最新
        assertThat(afterSecond.get(0).getRuleVersion()).isEqualTo("v2");
        assertThat(afterSecond.get(0).getScore()).isEqualTo(100);
    }

    @Test
    void assetLevelAndFieldLevelRowsCoexistAndListOnlyShowsAssetLevel() {
        HealthScore asset = calculate("asset-up2", null, "v1", allPassedRows("asset-up2"));
        HealthScore field = calculate("asset-up2", "name", "v1", fieldRow("asset-up2", "name"));
        healthScoreGateway.upsert(asset);
        healthScoreGateway.upsert(field);

        HealthScorePageQuery query = new HealthScorePageQuery();
        query.setAssetId("asset-up2");
        List<AssetHealthRowVO> rows = healthScoreGateway.listAssetHealth(query);

        assertThat(rows).hasSize(1); // 列表只含资产级行
        assertThat(rows.get(0).getAssetId()).isEqualTo("asset-up2");
        assertThat(rows.get(0).getBand()).isEqualTo(HealthBand.GOOD);
    }

    @Test
    void saveRuleDetailsPersistsWeightSnapshotAndIsIdempotent() {
        HealthScore asset = calculate("asset-up3", null, "v1", allPassedRows("asset-up3"));
        healthScoreGateway.upsert(asset);
        healthScoreGateway.saveRuleDetails(asset.getBatchId(), asset.getAssetId(), asset.getFieldName(),
                details(asset, allPassedRows("asset-up3")));

        RuleDetailVO detail = healthScoreGateway.findRuleDetail("asset-up3", null);
        assertThat(detail).isNotNull();
        assertThat(detail.getRules()).hasSize(5);
        assertThat(detail.getRules()).allSatisfy(r -> {
            assertThat(r.getWeight()).isNotNull();
            assertThat(r.getToolTime()).isNotNull();
        });
        assertThat(detail.getAlgorithm()).isNotNull();
        assertThat(detail.getAlgorithm().getFormula()).contains("Σ");
        assertThat(detail.getAlgorithm().getWeights()).hasSize(5);
        assertThat(detail.getAlgorithm().getWeights())
                .extracting(w -> w.getWeight())
                .containsExactly(0.25d, 0.25d, 0.20d, 0.15d, 0.15d);

        // 幂等可重算：同批次重写不产生重复行
        healthScoreGateway.saveRuleDetails(asset.getBatchId(), asset.getAssetId(), asset.getFieldName(),
                details(asset, allPassedRows("asset-up3")));
        RuleDetailVO again = healthScoreGateway.findRuleDetail("asset-up3", null);
        assertThat(again.getRules()).hasSize(5);
    }

    @Test
    void findLatestRuleVersionTracksIncrements() {
        assertThat(healthScoreGateway.findLatestRuleVersion("asset-up4", null)).isNull();
        healthScoreGateway.upsert(calculate("asset-up4", null, "v1", allPassedRows("asset-up4")));
        assertThat(healthScoreGateway.findLatestRuleVersion("asset-up4", null)).isEqualTo("v1");
        healthScoreGateway.upsert(calculate("asset-up4", null, "v2", allPassedRows("asset-up4")));
        assertThat(healthScoreGateway.findLatestRuleVersion("asset-up4", null)).isEqualTo("v2");
    }

    @Test
    void expiredRowDerivesExpiredStateAndNullBand() {
        Instant pastExecution = Instant.now().minus(31, ChronoUnit.DAYS);
        HealthScore expired = calculateWithExecution("asset-up5", null, "v1",
                allPassedRows("asset-up5"), pastExecution);
        healthScoreGateway.upsert(expired);

        HealthScorePageQuery query = new HealthScorePageQuery();
        query.setAssetId("asset-up5");
        List<AssetHealthRowVO> rows = healthScoreGateway.listAssetHealth(query);

        assertThat(rows).hasSize(1);
        AssetHealthRowVO row = rows.get(0);
        assertThat(row.getState()).isEqualTo(HealthState.EXPIRED);
        assertThat(row.isExpired()).isTrue();
        assertThat(row.getBand()).isNull();
        assertThat(row.getValidUntil()).isNotNull();
        assertThat(row.getScore()).isNotNull(); // 分数保留用于标灰展示
    }

    @Test
    void freshRowDerivesOkStateWithBand() {
        healthScoreGateway.upsert(calculate("asset-up6", null, "v1", allPassedRows("asset-up6")));

        HealthScorePageQuery query = new HealthScorePageQuery();
        query.setAssetId("asset-up6");
        List<AssetHealthRowVO> rows = healthScoreGateway.listAssetHealth(query);

        assertThat(rows).hasSize(1);
        AssetHealthRowVO row = rows.get(0);
        assertThat(row.getState()).isEqualTo(HealthState.OK);
        assertThat(row.isExpired()).isFalse();
        assertThat(row.getBand()).isEqualTo(HealthBand.GOOD);
        assertThat(row.isHasResult()).isTrue();
        assertThat(row.getPassRate()).isEqualTo("100%");
    }

    @Test
    void bandFilterExcludesExpiredRowsAndExpiredFilterFindsThem() {
        healthScoreGateway.upsert(calculate("asset-up7", null, "v1", allPassedRows("asset-up7")));
        Instant pastExecution = Instant.now().minus(31, ChronoUnit.DAYS);
        healthScoreGateway.upsert(calculateWithExecution("asset-up8", null, "v1",
                allPassedRows("asset-up8"), pastExecution));

        HealthScorePageQuery good = new HealthScorePageQuery();
        good.setBand(BandFilter.GOOD);
        assertThat(healthScoreGateway.listAssetHealth(good))
                .extracting(AssetHealthRowVO::getAssetId)
                .containsExactly("asset-up7"); // 过期行不归入档位

        HealthScorePageQuery expired = new HealthScorePageQuery();
        expired.setBand(BandFilter.EXPIRED);
        assertThat(healthScoreGateway.listAssetHealth(expired))
                .extracting(AssetHealthRowVO::getAssetId)
                .containsExactly("asset-up8");
    }

    @Test
    void noresultFilterReturnsEmptyPage() {
        healthScoreGateway.upsert(calculate("asset-up9", null, "v1", allPassedRows("asset-up9")));

        HealthScorePageQuery noresult = new HealthScorePageQuery();
        noresult.setBand(BandFilter.NORESULT);
        List<AssetHealthRowVO> rows = healthScoreGateway.listAssetHealth(noresult);
        assertThat(rows).isEmpty();
        assertThat(noresult.getTempTotalCount()).isZero();
    }

    @Test
    void listFiltersByDomainAndAssetType() {
        HealthScore score = calculate("asset-up10", null, "v1", allPassedRows("asset-up10"));
        healthScoreGateway.upsert(score);

        HealthScorePageQuery domainQuery = new HealthScorePageQuery();
        domainQuery.setDomain("交易域");
        assertThat(healthScoreGateway.listAssetHealth(domainQuery)).hasSize(1);

        HealthScorePageQuery noMatch = new HealthScorePageQuery();
        noMatch.setDomain("不存在的域");
        assertThat(healthScoreGateway.listAssetHealth(noMatch)).isEmpty();
        assertThat(noMatch.getTempTotalCount()).isZero();
    }

    @Test
    void findAssetHealthDetailReturnsSummaryFieldsAndLowScoreFlag() {
        Long batchId = savedBatch("health-detail-batch-11");
        // 资产级 + 字段级（name 高分 / amount 低分 score < 75）
        HealthScore asset = calculateWithBatch("asset-up11", null, "v1", allPassedRows("asset-up11"), batchId);
        HealthScore goodField = calculateWithBatch("asset-up11", "name", "v1",
                allPassedRowsForField("asset-up11", "name"), batchId);
        HealthScore poorField = calculateWithBatch("asset-up11", "amount", "v1",
                fieldRow("asset-up11", "amount"), batchId);
        healthScoreGateway.upsert(asset);
        healthScoreGateway.upsert(goodField);
        healthScoreGateway.upsert(poorField);
        healthScoreGateway.saveRuleDetails(batchId, asset.getAssetId(), asset.getFieldName(),
                details(asset, allPassedRows("asset-up11")));
        healthScoreGateway.saveRuleDetails(batchId, goodField.getAssetId(), goodField.getFieldName(),
                details(goodField, allPassedRowsForField("asset-up11", "name")));
        healthScoreGateway.saveRuleDetails(batchId, poorField.getAssetId(), poorField.getFieldName(),
                details(poorField, fieldRow("asset-up11", "amount")));

        AssetHealthDetailVO detail = healthScoreGateway.findAssetHealthDetail("asset-up11");

        assertThat(detail).isNotNull();
        assertThat(detail.getRuleVersion()).isEqualTo("v1");
        assertThat(detail.getScore()).isEqualTo(100);
        assertThat(detail.getBand()).isEqualTo(HealthBand.GOOD);
        assertThat(detail.getSourceTool()).isEqualTo(SourceTool.GREAT_EXPECTATIONS);
        assertThat(detail.getFields()).hasSize(2);
        assertThat(detail.getFields()).extracting(FieldHealthVO::isLowScore)
                .containsExactlyInAnyOrder(false, true);
        assertThat(detail.getFields()).extracting(FieldHealthVO::getFieldName)
                .containsExactlyInAnyOrder("name", "amount");
        assertThat(detail.getFields()).filteredOn(f -> f.getFieldName().equals("name"))
                .singleElement().satisfies(f -> {
                    assertThat(f.isLowScore()).isFalse();
                    assertThat(f.getRuleCount()).isEqualTo(5);
                });
        assertThat(detail.getFields()).filteredOn(f -> f.getFieldName().equals("amount"))
                .singleElement().satisfies(f -> {
                    assertThat(f.isLowScore()).isTrue();
                    assertThat(f.getRuleCount()).isEqualTo(1);
                });
    }

    @Test
    void findAssetHealthDetailReturnsNullForUnknownAsset() {
        assertThat(healthScoreGateway.findAssetHealthDetail("asset-unknown")).isNull();
    }

    @Test
    void findRuleDetailSupportsFieldFilter() {
        HealthScore asset = calculate("asset-up12", null, "v1", allPassedRows("asset-up12"));
        HealthScore field = calculate("asset-up12", "name", "v1", fieldRow("asset-up12", "name"));
        healthScoreGateway.upsert(asset);
        healthScoreGateway.upsert(field);
        healthScoreGateway.saveRuleDetails(asset.getBatchId(), asset.getAssetId(), asset.getFieldName(),
                details(asset, allPassedRows("asset-up12")));
        healthScoreGateway.saveRuleDetails(field.getBatchId(), field.getAssetId(), field.getFieldName(),
                details(field, fieldRow("asset-up12", "name")));

        RuleDetailVO assetDetail = healthScoreGateway.findRuleDetail("asset-up12", null);
        assertThat(assetDetail.getFieldName()).isNull();
        assertThat(assetDetail.getRules()).hasSize(5);

        RuleDetailVO fieldDetail = healthScoreGateway.findRuleDetail("asset-up12", "name");
        assertThat(fieldDetail.getFieldName()).isEqualTo("name");
        assertThat(fieldDetail.getRules()).hasSize(1);

        assertThat(healthScoreGateway.findRuleDetail("asset-up12", "no-such-field")).isNull();
    }

    @Test
    void paginationLimitsRowsAndReportsTotal() {
        healthScoreGateway.upsert(calculate("asset-pg1", null, "v1", allPassedRows("asset-pg1")));
        healthScoreGateway.upsert(calculate("asset-pg2", null, "v1", allPassedRows("asset-pg2")));
        healthScoreGateway.upsert(calculate("asset-pg3", null, "v1", allPassedRows("asset-pg3")));

        HealthScorePageQuery query = new HealthScorePageQuery();
        query.setPageIndex(1);
        query.setPageSize(2);
        List<AssetHealthRowVO> rows = healthScoreGateway.listAssetHealth(query);

        assertThat(rows).hasSize(2);
        assertThat(query.getTempTotalCount()).isEqualTo(3);
    }

    private static HealthScore calculate(String assetId, String fieldName, String version,
            List<RuleResultRow> rows) {
        return calculateWithExecution(assetId, fieldName, version, rows, EXECUTION_TIME);
    }

    private static HealthScore calculateWithExecution(String assetId, String fieldName, String version,
            List<RuleResultRow> rows, Instant executionTime) {
        return HealthScore.calculate(assetId, fieldName, "用户表", "交易域", "table",
                1L, executionTime, version, rows);
    }

    private static HealthScore calculateWithBatch(String assetId, String fieldName, String version,
            List<RuleResultRow> rows, Long batchId) {
        return HealthScore.calculate(assetId, fieldName, "用户表", "交易域", "table",
                batchId, EXECUTION_TIME, version, rows);
    }

    private Long savedBatch(String batchNo) {
        DQResultBatch batch = DQResultBatch.createIngested(batchNo, SourceTool.GREAT_EXPECTATIONS,
                FormatType.GE, "ch-1", EXECUTION_TIME,
                Collections.emptyList());
        DQResultBatch saved = dqResultGateway.save(batch, Collections.emptyList(), Collections.emptyList());
        return saved.getId();
    }

    private static List<RuleResultRow> allPassedRows(String assetId) {
        return Arrays.asList(
                row(assetId, null, "非空率", RuleType.NON_NULL_RATE, RuleStatus.PASSED),
                row(assetId, null, "格式", RuleType.FORMAT, RuleStatus.PASSED),
                row(assetId, null, "唯一性", RuleType.UNIQUENESS, RuleStatus.PASSED),
                row(assetId, null, "值域", RuleType.VALUE_RANGE, RuleStatus.PASSED),
                row(assetId, null, "新鲜度", RuleType.FRESHNESS, RuleStatus.PASSED));
    }

    private static List<RuleResultRow> allPassedRowsForField(String assetId, String fieldName) {
        return Arrays.asList(
                row(assetId, fieldName, "非空率-" + fieldName, RuleType.NON_NULL_RATE, RuleStatus.PASSED),
                row(assetId, fieldName, "格式-" + fieldName, RuleType.FORMAT, RuleStatus.PASSED),
                row(assetId, fieldName, "唯一性-" + fieldName, RuleType.UNIQUENESS, RuleStatus.PASSED),
                row(assetId, fieldName, "值域-" + fieldName, RuleType.VALUE_RANGE, RuleStatus.PASSED),
                row(assetId, fieldName, "新鲜度-" + fieldName, RuleType.FRESHNESS, RuleStatus.PASSED));
    }

    private static List<RuleResultRow> fieldRow(String assetId, String fieldName) {
        return Collections.singletonList(
                row(assetId, fieldName, "非空率-" + fieldName, RuleType.NON_NULL_RATE,
                        fieldName.equals("amount") ? RuleStatus.FAILED : RuleStatus.PASSED));
    }

    private static RuleResultRow row(String assetId, String fieldName, String ruleName, RuleType ruleType,
            RuleStatus status) {
        return RuleResultRow.builder()
                .assetId(assetId)
                .fieldName(fieldName)
                .ruleName(ruleName)
                .ruleType(ruleType)
                .status(status)
                .executionTime(EXECUTION_TIME)
                .build();
    }

    private static List<RuleScoreSnapshot> details(HealthScore score, List<RuleResultRow> rows) {
        List<RuleScoreSnapshot> details = new ArrayList<>(rows.size());
        for (RuleResultRow row : rows) {
            details.add(RuleScoreSnapshot.of(score, row, 0.25d));
        }
        return details;
    }
}
