package com.yss.datamiddle.dqinsight.core.service.impl;

import com.yss.datamiddle.dqinsight.core.service.HealthScoreCalculationAppService;
import com.yss.datamiddle.dqinsight.domain.gateway.AuditLogGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.DqResultGateway;
import com.yss.datamiddle.dqinsight.domain.model.AssetLinkage;
import com.yss.datamiddle.dqinsight.domain.model.AssetSnapshot;
import com.yss.datamiddle.dqinsight.domain.model.AuditLogEntry;
import com.yss.datamiddle.dqinsight.domain.model.DQResultBatch;
import com.yss.datamiddle.dqinsight.domain.model.LinkageState;
import com.yss.datamiddle.dqinsight.domain.model.RuleResultRow;
import com.yss.datamiddle.dqinsight.domain.service.HealthScoreEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 健康分计算用例编排实现（Application 只编排，C10）。
 *
 * <p>流程：读批次（执行时间 / 有效期）→ 读规则结果 → 读关联快照 → 按 (asset_id, field_name) 分组 →
 * 对关联命中资产执行规则加权计算（引擎在 Domain）→ 单聚合事务 upsert 健康分 + 规则明细快照 →
 * 审计 health-calc（独立 append-only，不参与计算事务，数据架构 §7）。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HealthScoreCalculationAppServiceImpl implements HealthScoreCalculationAppService {

    private static final String OPERATOR_SYSTEM = "system";

    private final DqResultGateway dqResultGateway;
    private final AuditLogGateway auditLogGateway;
    private final HealthScorePersistenceService healthScorePersistenceService;
    private final HealthScoreEngine healthScoreEngine = new HealthScoreEngine();

    @Override
    public void calculateForAssets(String batchId, List<String> assetIds) {
        if (batchId == null || batchId.trim().isEmpty()
                || assetIds == null || assetIds.isEmpty()) {
            return;
        }
        Long batchIdValue;
        try {
            batchIdValue = Long.valueOf(batchId.trim());
        } catch (NumberFormatException e) {
            log.warn("健康分计算触发：非法批次 ID {}", batchId);
            return;
        }
        DQResultBatch batch = dqResultGateway.findBatchById(batchIdValue);
        if (batch == null) {
            log.warn("健康分计算触发：批次不存在 batchId={}", batchIdValue);
            return;
        }
        List<RuleResultRow> rows = dqResultGateway.findRuleResultsByBatchId(batchIdValue);
        if (rows.isEmpty()) {
            log.info("健康分计算：批次无规则结果，跳过 batchId={}", batchIdValue);
            return;
        }
        Map<String, AssetSnapshot> snapshots = linkedAssetSnapshots(batchIdValue);
        if (snapshots.isEmpty()) {
            log.info("健康分计算：批次无关联命中资产，跳过 batchId={}", batchIdValue);
            return;
        }

        Set<String> targets = new LinkedHashSet<>(assetIds);
        Map<String, List<RuleResultRow>> groups = groupByAssetAndField(rows);
        List<String> versions = new ArrayList<>();
        int computedCount = 0;

        // 按 资产 → 字段 分组计算（资产级 fieldName=null 与字段级共用分组，一并计算）
        for (Map.Entry<String, List<RuleResultRow>> group : groups.entrySet()) {
            List<RuleResultRow> groupRows = group.getValue();
            if (groupRows.isEmpty()) {
                continue;
            }
            String assetId = groupRows.get(0).getAssetId();
            String fieldName = groupRows.get(0).getFieldName();
            if (!targets.contains(assetId)) {
                continue;
            }
            AssetSnapshot snapshot = snapshots.get(assetId);
            if (snapshot == null) {
                continue;
            }
            // 健康行按快照解析资产 ID 落主平台口径（人工映射 sourceAssetId ≠ resolvedAssetId 时
            // 健康分挂在解析后资产下；自动关联 source == resolved，行为不变）
            String version = healthScorePersistenceService.saveComputation(
                    batch, snapshot.getAssetId(), fieldName, snapshot, groupRows);
            versions.add(version);
            computedCount++;
        }

        if (computedCount > 0) {
            auditLogGateway.record(AuditLogEntry.healthCalc(OPERATOR_SYSTEM, batch.getBatchNo(),
                    "batchNo=" + batch.getBatchNo() + ", sourceTool=" + batch.getSourceTool().getCode()
                            + ", assetCount=" + computedCount + ", ruleVersion=" + String.join(",", versions)));
        }
    }

    /**
     * 关联命中资产快照（LINKED 关联的名称 / 域 / 类型）。
     */
    private Map<String, AssetSnapshot> linkedAssetSnapshots(Long batchId) {
        List<AssetLinkage> linkages = dqResultGateway.findLinkagesByBatchId(batchId);
        Map<String, AssetSnapshot> snapshots = new HashMap<>();
        for (AssetLinkage linkage : linkages) {
            if (linkage.getState() == LinkageState.LINKED && linkage.getResolvedAssetId() != null) {
                snapshots.put(linkage.getSourceAssetId(), AssetSnapshot.builder()
                        .assetId(linkage.getResolvedAssetId())
                        .assetName(linkage.getAssetName())
                        .domain(linkage.getDomain())
                        .assetType(linkage.getAssetType())
                        .build());
            }
        }
        return snapshots;
    }

    /**
     * 按 (asset_id, field_name) 分组规则结果（保留原顺序，fieldName null = 资产级）。
     */
    private Map<String, List<RuleResultRow>> groupByAssetAndField(List<RuleResultRow> rows) {
        Map<String, List<RuleResultRow>> groups = new HashMap<>();
        for (RuleResultRow row : rows) {
            if (row.getAssetId() == null || row.getAssetId().trim().isEmpty()) {
                continue;
            }
            String key = row.getAssetId() + "|" + (row.getFieldName() == null ? "" : row.getFieldName());
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }
        return groups;
    }
}
