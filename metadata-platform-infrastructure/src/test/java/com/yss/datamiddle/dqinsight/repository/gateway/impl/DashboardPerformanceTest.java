package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.yss.datamiddle.dqinsight.InfraTestApplication;
import com.yss.datamiddle.dqinsight.client.dto.query.HealthScorePageQuery;
import com.yss.datamiddle.dqinsight.domain.gateway.CatalogAclGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.DashboardGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.HealthScoreGateway;
import com.yss.datamiddle.dqinsight.repository.DqHealthScoreRepository;
import com.yss.datamiddle.dqinsight.repository.entity.DqHealthScorePO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

/**
 * 仪表盘聚合 / 列表性能验证（DQI-SLICE-03-WU1，C28 / SB-10 量级）。
 *
 * <p>量级回放：资产级 + 字段级健康分行合计 4000 行（目标资产 ≤ 1 万量级下限），
 * 分页筛选请求 P95 &lt; 1s。H2（MySQL 模式）内存库 + 索引覆盖（V2 DDL (domain, band) /
 * (domain, state) / (band)）。本测试为量级回放证据，非压测工具替代；100 QPS 与生产 MySQL
 * 行为由发布前压测（P1 排程）验证。</p>
 */
@SpringBootTest(classes = InfraTestApplication.class)
@Transactional
class DashboardPerformanceTest {

    private static final int ASSET_ROWS = 2000;

    private static final int FIELD_ROWS = 2000;

    private static final int WARMUP = 5;

    private static final int SAMPLES = 30;

    @Autowired
    private DashboardGateway dashboardGateway;

    @Autowired
    private HealthScoreGateway healthScoreGateway;

    @Autowired
    private DqHealthScoreRepository dqHealthScoreRepository;

    @MockBean
    private CatalogAclGateway catalogAclGateway;

    @BeforeEach
    void setUp() {
        when(catalogAclGateway.countVisibleTargetAssets(nullable(String.class))).thenReturn(10000);
        seedRows();
    }

    @Test
    void dashboardAggregationAndListP95UnderOneSecond() {
        for (int i = 0; i < WARMUP; i++) {
            measureMillis(); // 预热（JIT / 连接 / 分页切面初始化）
        }
        List<Long> samples = new ArrayList<>(SAMPLES);
        for (int i = 0; i < SAMPLES; i++) {
            samples.add(measureMillis());
        }
        Collections.sort(samples);
        long p95 = samples.get((int) Math.ceil(SAMPLES * 0.95) - 1);
        long max = samples.get(SAMPLES - 1);

        System.out.println("[dashboard-perf] rows=" + (ASSET_ROWS + FIELD_ROWS)
                + " samples=" + SAMPLES + " p95=" + p95 + "ms max=" + max + "ms");
        assertThat(p95).isLessThan(1000L);
    }

    private long measureMillis() {
        HealthScorePageQuery query = new HealthScorePageQuery();
        query.setPageIndex(1);
        query.setPageSize(20);
        long start = System.nanoTime();
        dashboardGateway.loadStats(query);
        healthScoreGateway.listAssetHealth(query);
        return (System.nanoTime() - start) / 1_000_000L;
    }

    private void seedRows() {
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < ASSET_ROWS; i++) {
            DqHealthScorePO po = new DqHealthScorePO();
            po.setAssetId("perf-asset-" + i);
            po.setFieldName(null);
            po.setAssetName("性能资产-" + i);
            po.setDomain(i % 2 == 0 ? "交易域" : "风控域");
            po.setAssetType("table");
            po.setScore(i % 3 == 0 ? 60 : 100);
            po.setHealthBand(i % 3 == 0 ? "差" : "优");
            po.setState("ok");
            po.setRuleVersion("v1");
            po.setBatchId(1L);
            po.setComputedAt(now);
            po.setLastResultAt(now);
            po.setValidUntil(now.plusDays(30));
            po.setPassRate("100%");
            dqHealthScoreRepository.insert(po);
        }
        for (int i = 0; i < FIELD_ROWS; i++) {
            DqHealthScorePO po = new DqHealthScorePO();
            po.setAssetId("perf-asset-" + (i % ASSET_ROWS));
            po.setFieldName("field-" + i);
            po.setAssetName("性能资产-" + (i % ASSET_ROWS));
            po.setDomain("交易域");
            po.setAssetType("table");
            po.setScore(100);
            po.setHealthBand("优");
            po.setState("ok");
            po.setRuleVersion("v1");
            po.setBatchId(1L);
            po.setComputedAt(now);
            po.setLastResultAt(now);
            po.setValidUntil(now.plusDays(30));
            po.setPassRate("100%");
            dqHealthScoreRepository.insert(po);
        }
    }
}
