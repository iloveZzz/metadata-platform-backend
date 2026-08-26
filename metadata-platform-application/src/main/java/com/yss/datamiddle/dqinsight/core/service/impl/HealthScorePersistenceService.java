package com.yss.datamiddle.dqinsight.core.service.impl;

import com.yss.datamiddle.dqinsight.domain.gateway.HealthScoreGateway;
import com.yss.datamiddle.dqinsight.domain.model.AssetSnapshot;
import com.yss.datamiddle.dqinsight.domain.model.DQResultBatch;
import com.yss.datamiddle.dqinsight.domain.model.HealthScore;
import com.yss.datamiddle.dqinsight.domain.model.RuleResultRow;
import com.yss.datamiddle.dqinsight.domain.model.RuleScoreSnapshot;
import com.yss.datamiddle.dqinsight.domain.service.HealthScoreEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 健康分计算持久化服务（单聚合事务边界：dq_health_score upsert + dq_rule_detail 快照，计算幂等可重算）。
 *
 * <p>独立 Bean 以保证 @Transactional 代理生效；审计 health-calc 独立 append-only 不参与本事务
 * （数据架构 §7）；ruleVersion 在事务内读取上次版本递增（v1 → v2，C22）。</p>
 */
@Service
@RequiredArgsConstructor
public class HealthScorePersistenceService {

    private final HealthScoreGateway healthScoreGateway;
    private final HealthScoreEngine healthScoreEngine = new HealthScoreEngine();

    /**
     * 单聚合事务：按 (asset_id, field_name) upsert 健康分 + 规则明细快照；返回本次计算规则版本。
     */
    @Transactional(rollbackFor = Exception.class)
    public String saveComputation(DQResultBatch batch, String assetId, String fieldName,
            AssetSnapshot snapshot, List<RuleResultRow> rows) {
        String latestVersion = healthScoreGateway.findLatestRuleVersion(assetId, fieldName);
        String version = healthScoreEngine.nextVersion(latestVersion);
        HealthScore score = HealthScore.calculate(assetId, fieldName,
                snapshot.getAssetName(), snapshot.getDomain(), snapshot.getAssetType(),
                batch.getId(), batch.getExecutionTime(), version, rows);
        healthScoreGateway.upsert(score);

        List<RuleScoreSnapshot> details = new ArrayList<>(rows.size());
        for (RuleResultRow row : rows) {
            details.add(RuleScoreSnapshot.of(score, row, healthScoreEngine.weightOf(row.getRuleType())));
        }
        healthScoreGateway.saveRuleDetails(batch.getId(), assetId, fieldName, details);
        return version;
    }
}
