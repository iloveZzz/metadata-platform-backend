package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.yss.datamiddle.dqinsight.InfraTestApplication;
import com.yss.datamiddle.dqinsight.domain.gateway.BatchExpiryGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.DqResultGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.HealthScoreGateway;
import com.yss.datamiddle.dqinsight.domain.model.DQResultBatch;
import com.yss.datamiddle.dqinsight.domain.model.ErrorCategory;
import com.yss.datamiddle.dqinsight.domain.model.FormatType;
import com.yss.datamiddle.dqinsight.domain.model.HealthScore;
import com.yss.datamiddle.dqinsight.domain.model.IngestionStatus;
import com.yss.datamiddle.dqinsight.domain.model.RuleResultRow;
import com.yss.datamiddle.dqinsight.domain.model.RuleStatus;
import com.yss.datamiddle.dqinsight.domain.model.RuleType;
import com.yss.datamiddle.dqinsight.domain.model.SourceTool;
import com.yss.datamiddle.dqinsight.repository.DqBatchRepository;
import com.yss.datamiddle.dqinsight.repository.entity.DqBatchPO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 健康分有效期 / 过期流转集成测试（DQI-SLICE-02-WU2，C23）。
 *
 * <p>低频调度将超期批次（valid_until &lt; now 且 status = ingested）置 invalidated（幂等可重跑）；
 * 健康分过期展示态由查询投影派生（expired=true + validUntil），与「无结果」独立展示态不混淆；
 * 重新接入新批次后重算恢复档位。</p>
 */
@SpringBootTest(classes = InfraTestApplication.class)
@Transactional
class HealthExpiryFlowTest {

    @Autowired
    private BatchExpiryGateway batchExpiryGateway;

    @Autowired
    private DqResultGateway dqResultGateway;

    @Autowired
    private HealthScoreGateway healthScoreGateway;

    @Autowired
    private DqBatchRepository dqBatchRepository;

    @Test
    void invalidateExpiredFlipsOnlyIngestedPastValidityBatches() {
        // 超期已入库批次
        DQResultBatch expiredIngested = DQResultBatch.createIngested("expiry-1",
                SourceTool.GREAT_EXPECTATIONS, FormatType.GE, "ch-1",
                Instant.now().minus(31, ChronoUnit.DAYS), Collections.emptyList());
        dqResultGateway.save(expiredIngested, Collections.emptyList(), Collections.emptyList());

        // 未超期已入库批次
        DQResultBatch freshIngested = DQResultBatch.createIngested("expiry-2",
                SourceTool.GREAT_EXPECTATIONS, FormatType.GE, "ch-1",
                Instant.now(), Collections.emptyList());
        dqResultGateway.save(freshIngested, Collections.emptyList(), Collections.emptyList());

        // 超期解析失败批次（不参与流转）
        DQResultBatch parseFailed = DQResultBatch.createParseFailed(FormatType.CSV, SourceTool.GENERIC,
                "expiry-3", null, ErrorCategory.FORMAT, "schema 违反");
        dqResultGateway.save(parseFailed, Collections.emptyList(), Collections.emptyList());

        int affected = batchExpiryGateway.invalidateExpired(Instant.now());

        assertThat(affected).isEqualTo(1);
        assertThat(statusOf("expiry-1")).isEqualTo(IngestionStatus.INVALIDATED.getCode());
        assertThat(statusOf("expiry-2")).isEqualTo(IngestionStatus.INGESTED.getCode());
        assertThat(statusOf("expiry-3")).isEqualTo(IngestionStatus.PARSE_FAILED.getCode());
    }

    @Test
    void invalidateExpiredIsIdempotentAndRerunnable() {
        DQResultBatch expiredIngested = DQResultBatch.createIngested("expiry-idem",
                SourceTool.GREAT_EXPECTATIONS, FormatType.GE, "ch-1",
                Instant.now().minus(31, ChronoUnit.DAYS), Collections.emptyList());
        dqResultGateway.save(expiredIngested, Collections.emptyList(), Collections.emptyList());

        assertThat(batchExpiryGateway.invalidateExpired(Instant.now())).isEqualTo(1);
        // 第二次执行匹配 0 行（幂等可重跑）
        assertThat(batchExpiryGateway.invalidateExpired(Instant.now())).isZero();
        assertThat(statusOf("expiry-idem")).isEqualTo(IngestionStatus.INVALIDATED.getCode());
    }

    @Test
    void expiredHealthScoreIsDerivedAtQueryTimeAndNotConfusedWithNoResult() {
        // 超期健康分（执行时间 31 天前 → validUntil 已过）
        HealthScore expired = HealthScore.calculate("expiry-asset-1", null, "用户表", "交易域", "table",
                1L, Instant.now().minus(31, ChronoUnit.DAYS), "v1", Collections.emptyList());
        healthScoreGateway.upsert(expired);

        // 未超期健康分
        HealthScore fresh = HealthScore.calculate("expiry-asset-2", null, "订单表", "交易域", "table",
                1L, Instant.now(), "v1", Collections.emptyList());
        healthScoreGateway.upsert(fresh);

        List<com.yss.datamiddle.dqinsight.client.vo.AssetHealthRowVO> rows =
                healthScoreGateway.listAssetHealth(new com.yss.datamiddle.dqinsight.client.dto.query.HealthScorePageQuery());

        assertThat(rows).hasSize(2);
        com.yss.datamiddle.dqinsight.client.vo.AssetHealthRowVO expiredRow = rows.stream()
                .filter(r -> r.getAssetId().equals("expiry-asset-1"))
                .findFirst().orElseThrow(AssertionError::new);
        com.yss.datamiddle.dqinsight.client.vo.AssetHealthRowVO freshRow = rows.stream()
                .filter(r -> r.getAssetId().equals("expiry-asset-2"))
                .findFirst().orElseThrow(AssertionError::new);

        // 过期独立展示态：state=expired / expired=true / band=null / validUntil 已过；与 noresult 不混淆
        assertThat(expiredRow.getState().getCode()).isEqualTo("expired");
        assertThat(expiredRow.isExpired()).isTrue();
        assertThat(expiredRow.getBand()).isNull();
        assertThat(expiredRow.getValidUntil()).isNotNull();
        assertThat(expiredRow.isHasResult()).isTrue(); // 过期仍有结果（含过期计入已接入，SB-07）

        assertThat(freshRow.getState().getCode()).isEqualTo("ok");
        assertThat(freshRow.isExpired()).isFalse();
    }

    @Test
    void reingestionRecomputesAndRestoresComputedBand() {
        // 超期批次入库 → 计算（过期态）
        DQResultBatch oldBatch = DQResultBatch.createIngested("recover-1",
                SourceTool.GREAT_EXPECTATIONS, FormatType.GE, "ch-1",
                Instant.now().minus(31, ChronoUnit.DAYS), Collections.emptyList());
        dqResultGateway.save(oldBatch, Collections.emptyList(), Collections.emptyList());
        HealthScore expired = HealthScore.calculate("recover-asset", null, "用户表", "交易域", "table",
                oldBatch.getId(), Instant.now().minus(31, ChronoUnit.DAYS), "v1",
                Collections.emptyList());
        healthScoreGateway.upsert(expired);

        // 重新接入新批次 → 重算（v2）→ 过期态恢复为已计算档位
        DQResultBatch newBatch = DQResultBatch.createIngested("recover-2",
                SourceTool.GREAT_EXPECTATIONS, FormatType.GE, "ch-1",
                Instant.now(), Collections.emptyList());
        dqResultGateway.save(newBatch, Collections.emptyList(), Collections.emptyList());
        List<RuleResultRow> rows = allPassed("recover-asset");
        HealthScore recomputed = HealthScore.calculate("recover-asset", null, "用户表", "交易域", "table",
                newBatch.getId(), Instant.now(), "v2", rows);
        healthScoreGateway.upsert(recomputed);

        assertThat(healthScoreGateway.findLatestRuleVersion("recover-asset", null)).isEqualTo("v2");

        com.yss.datamiddle.dqinsight.client.dto.query.HealthScorePageQuery query =
                new com.yss.datamiddle.dqinsight.client.dto.query.HealthScorePageQuery();
        query.setAssetId("recover-asset");
        com.yss.datamiddle.dqinsight.client.vo.AssetHealthRowVO row =
                healthScoreGateway.listAssetHealth(query).get(0);

        assertThat(row.getState().getCode()).isEqualTo("ok");
        assertThat(row.isExpired()).isFalse();
        assertThat(row.getBand().getCode()).isEqualTo("优");
        assertThat(row.getScore()).isEqualTo(100);
    }

    private String statusOf(String batchNo) {
        DqBatchPO po = dqBatchRepository.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DqBatchPO>()
                        .eq(DqBatchPO::getBatchNo, batchNo)).get(0);
        return po.getStatus();
    }

    private static List<RuleResultRow> allPassed(String assetId) {
        Instant executionTime = Instant.now();
        return java.util.Arrays.asList(
                row(assetId, "非空率", RuleType.NON_NULL_RATE, RuleStatus.PASSED, executionTime),
                row(assetId, "格式", RuleType.FORMAT, RuleStatus.PASSED, executionTime),
                row(assetId, "唯一性", RuleType.UNIQUENESS, RuleStatus.PASSED, executionTime),
                row(assetId, "值域", RuleType.VALUE_RANGE, RuleStatus.PASSED, executionTime),
                row(assetId, "新鲜度", RuleType.FRESHNESS, RuleStatus.PASSED, executionTime));
    }

    private static RuleResultRow row(String assetId, String ruleName, RuleType ruleType, RuleStatus status,
            Instant executionTime) {
        return RuleResultRow.builder()
                .assetId(assetId)
                .ruleName(ruleName)
                .ruleType(ruleType)
                .status(status)
                .executionTime(executionTime)
                .build();
    }
}
