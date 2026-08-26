package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.yss.datamiddle.dqinsight.InfraTestApplication;
import com.yss.datamiddle.dqinsight.client.dto.query.HealthScorePageQuery;
import com.yss.datamiddle.dqinsight.client.vo.DashboardStatsVO;
import com.yss.datamiddle.dqinsight.domain.gateway.CatalogAclGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.DashboardGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.DataDomainFilter;
import com.yss.datamiddle.dqinsight.domain.gateway.HealthScoreGateway;
import com.yss.datamiddle.dqinsight.domain.model.BandFilter;
import com.yss.datamiddle.dqinsight.domain.model.HealthBand;
import com.yss.datamiddle.dqinsight.domain.model.HealthScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

/**
 * 仪表盘聚合仓储集成测试（DQI-SLICE-03-WU1，C28 聚合语义 + C24 域过滤 seam）。
 *
 * <p>覆盖：bandDistribution（优 / 良 / 差 + 过期独立展示态）/ 已接入（含过期）/ 低分 / 覆盖率
 * （SB-07，targetAssetCount 来自防腐层 mock）/ 统计口径 = 数据域内可见资产全集
 * （DataDomainFilter seam + domain / assetType 筛选，不含 band）/ 字段级行不计入聚合。</p>
 */
@SpringBootTest(classes = InfraTestApplication.class)
@Transactional
class DashboardGatewayTest {

    @Autowired
    private DashboardGateway dashboardGateway;

    @Autowired
    private HealthScoreGateway healthScoreGateway;

    @MockBean
    private CatalogAclGateway catalogAclGateway;

    @MockBean
    private DataDomainFilter dataDomainFilter;

    private static final Instant EXECUTION_TIME = Instant.parse("2026-08-11T10:00:00Z");

    private static final Instant PAST_EXECUTION_TIME = Instant.now().minus(31, ChronoUnit.DAYS);

    @BeforeEach
    void setUp() {
        when(catalogAclGateway.countVisibleTargetAssets(nullable(String.class))).thenReturn(10);
        when(dataDomainFilter.visibleDomains()).thenReturn(Collections.emptyList());
    }

    @Test
    void loadStatsCountsBandsIngestedLowScoreAndCoverage() {
        // 交易域 table：优 ×3 / 良 ×2 / 差 ×1 / 过期 ×1 + 字段级 ×1（不计入聚合）
        upsert("dash-a1", "交易域", "table", 100, HealthBand.GOOD, EXECUTION_TIME);
        upsert("dash-a2", "交易域", "table", 100, HealthBand.GOOD, EXECUTION_TIME);
        upsert("dash-a3", "交易域", "table", 100, HealthBand.GOOD, EXECUTION_TIME);
        upsert("dash-b1", "交易域", "table", 80, HealthBand.FAIR, EXECUTION_TIME);
        upsert("dash-b2", "交易域", "table", 80, HealthBand.FAIR, EXECUTION_TIME);
        upsert("dash-c1", "交易域", "table", 60, HealthBand.POOR, EXECUTION_TIME);
        upsert("dash-exp", "交易域", "table", 100, HealthBand.GOOD, PAST_EXECUTION_TIME);
        upsertField("dash-f1", "name", "交易域", "table");

        DashboardStatsVO stats = dashboardGateway.loadStats(new HealthScorePageQuery());

        assertThat(stats.getBandDistribution().getGood()).isEqualTo(3);
        assertThat(stats.getBandDistribution().getFair()).isEqualTo(2);
        assertThat(stats.getBandDistribution().getPoor()).isEqualTo(1);
        assertThat(stats.getBandDistribution().getExpired()).isEqualTo(1);
        assertThat(stats.getBandDistribution().getNoResult()).isEqualTo(3); // 10 − 7
        assertThat(stats.getIngestedAssetCount()).isEqualTo(7); // 含过期
        assertThat(stats.getLowScoreAssetCount()).isEqualTo(1); // 差档非过期
        assertThat(stats.getTargetAssetCount()).isEqualTo(10);
        assertThat(stats.getCoverage()).isCloseTo(70.0f, within(0.001f));
    }

    @Test
    void loadStatsAppliesVisibleDomainSeamAndExcludesOutOfDomain() {
        upsert("dash-d1", "交易域", "table", 100, HealthBand.GOOD, EXECUTION_TIME);
        upsert("dash-d2", "交易域", "table", 100, HealthBand.GOOD, EXECUTION_TIME);
        upsert("dash-r1", "风控域", "table", 60, HealthBand.POOR, EXECUTION_TIME);
        upsert("dash-r2", "风控域", "table", 60, HealthBand.POOR, EXECUTION_TIME);
        when(dataDomainFilter.visibleDomains()).thenReturn(Collections.singletonList("交易域"));

        DashboardStatsVO stats = dashboardGateway.loadStats(new HealthScorePageQuery());

        assertThat(stats.getBandDistribution().getGood()).isEqualTo(2);
        assertThat(stats.getBandDistribution().getPoor()).isZero(); // 风控域不展示（C24）
        assertThat(stats.getIngestedAssetCount()).isEqualTo(2);
        assertThat(stats.getLowScoreAssetCount()).isZero();
    }

    @Test
    void loadStatsAppliesDomainAndAssetTypeScope() {
        upsert("dash-s1", "交易域", "table", 100, HealthBand.GOOD, EXECUTION_TIME);
        upsert("dash-s2", "交易域", "view", 100, HealthBand.GOOD, EXECUTION_TIME);
        upsert("dash-s3", "风控域", "table", 100, HealthBand.GOOD, EXECUTION_TIME);

        HealthScorePageQuery query = new HealthScorePageQuery();
        query.setDomain("交易域");
        query.setAssetType("table");
        DashboardStatsVO stats = dashboardGateway.loadStats(query);

        assertThat(stats.getBandDistribution().getGood()).isEqualTo(1);
        assertThat(stats.getIngestedAssetCount()).isEqualTo(1);
    }

    @Test
    void loadStatsIgnoresBandFilterForStatsScope() {
        upsert("dash-bf1", "交易域", "table", 100, HealthBand.GOOD, EXECUTION_TIME);
        upsert("dash-bf2", "交易域", "table", 60, HealthBand.POOR, EXECUTION_TIME);

        HealthScorePageQuery query = new HealthScorePageQuery();
        query.setBand(BandFilter.GOOD); // 档位筛选仅作用于资产列表
        DashboardStatsVO stats = dashboardGateway.loadStats(query);

        assertThat(stats.getBandDistribution().getGood()).isEqualTo(1);
        assertThat(stats.getBandDistribution().getPoor()).isEqualTo(1);
        assertThat(stats.getIngestedAssetCount()).isEqualTo(2);
    }

    @Test
    void zeroTargetYieldsZeroCoverage() {
        when(catalogAclGateway.countVisibleTargetAssets(nullable(String.class))).thenReturn(0);

        DashboardStatsVO stats = dashboardGateway.loadStats(new HealthScorePageQuery());

        assertThat(stats.getCoverage()).isZero();
        assertThat(stats.getTargetAssetCount()).isZero();
        assertThat(stats.getBandDistribution().getNoResult()).isZero();
    }

    private void upsert(String assetId, String domain, String assetType, int score,
            HealthBand band, Instant executionTime) {
        HealthScore scoreValue = HealthScore.calculate(assetId, null, "资产-" + assetId, domain,
                assetType, 1L, executionTime, "v1", Collections.emptyList());
        scoreValue.setScore(score);
        scoreValue.setBand(band);
        healthScoreGateway.upsert(scoreValue);
    }

    private void upsertField(String assetId, String fieldName, String domain, String assetType) {
        HealthScore field = HealthScore.calculate(assetId, fieldName, "资产-" + assetId, domain,
                assetType, 1L, EXECUTION_TIME, "v1", Collections.emptyList());
        field.setScore(100);
        field.setBand(HealthBand.GOOD);
        healthScoreGateway.upsert(field);
    }
}
