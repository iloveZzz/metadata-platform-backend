package com.yss.datamiddle.smartgovernance.application.service;

import com.yss.datamiddle.smartgovernance.domain.llm.LlmGateway;
import com.yss.datamiddle.smartgovernance.domain.metric.gateway.MetricConflictGateway;
import com.yss.datamiddle.smartgovernance.domain.metric.gateway.MetricReconciliationGateway;
import com.yss.datamiddle.smartgovernance.domain.metric.model.ConflictStatus;
import com.yss.datamiddle.smartgovernance.domain.metric.model.ConflictType;
import com.yss.datamiddle.smartgovernance.domain.metric.model.MetricAstDiff;
import com.yss.datamiddle.smartgovernance.domain.metric.model.MetricConflictRecord;
import com.yss.datamiddle.smartgovernance.domain.metric.model.MetricReconciliationLog;
import com.yss.datamiddle.smartgovernance.domain.metric.service.MetricAstComparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricGovernanceApplicationService {

    private final MetricConflictGateway conflictGateway;
    private final MetricReconciliationGateway reconciliationGateway;
    private final LlmGateway llmGateway;

    private final MetricAstComparator astComparator = new MetricAstComparator();

    public List<MetricConflictRecord> queryConflicts(
            Integer pageIndex,
            Integer pageSize,
            ConflictStatus status,
            ConflictType conflictType,
            String keyword
    ) {
        return conflictGateway.queryConflicts(pageIndex, pageSize, status, conflictType, keyword);
    }

    public long countConflicts(ConflictStatus status, ConflictType conflictType, String keyword) {
        return conflictGateway.countConflicts(status, conflictType, keyword);
    }

    /**
     * 触发指标语义与 AST 公式全量扫描任务
     */
    @Transactional(rollbackFor = Exception.class)
    public String triggerConflictScan() {
        List<MetricConflictRecord> detected = new ArrayList<>();

        // 模拟探测指标对 1：GMV 口径漂移
        String f1A = "sum(order_amount) where status in (1, 2) and pay_time >= today()";
        String f1B = "sum(order_amount) where status = 1 and pay_time >= today()";
        MetricAstDiff diff1 = astComparator.compareFormulas(f1A, f1B);

        detected.add(MetricConflictRecord.builder()
                .id(UUID.randomUUID().toString())
                .conflictCode("MC_" + System.currentTimeMillis() + "_01")
                .indicatorAId("ind-001")
                .indicatorAName("财务_全渠道GMV")
                .indicatorACode("fin_all_channel_gmv")
                .indicatorADomain("财务域")
                .indicatorBId("ind-002")
                .indicatorBName("运营_平台成交额")
                .indicatorBCode("ops_platform_turnover")
                .indicatorBDomain("运营域")
                .conflictType(diff1.getConflictType())
                .similarityScore(new BigDecimal(String.valueOf(diff1.getSimilarityScore())))
                .formulaA(f1A)
                .formulaB(f1B)
                .astDiffSummary(diff1.getAstSummary() + " | " + diff1.getWhereClauseDiff())
                .status(ConflictStatus.UNRESOLVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        // 模拟探测指标对 2：日活用户同名异义
        String f2A = "count(distinct user_id) where active_status = 1";
        String f2B = "count(user_id) where action_type = 'LOGIN'";
        MetricAstDiff diff2 = astComparator.compareFormulas(f2A, f2B);

        detected.add(MetricConflictRecord.builder()
                .id(UUID.randomUUID().toString())
                .conflictCode("MC_" + System.currentTimeMillis() + "_02")
                .indicatorAId("ind-003")
                .indicatorAName("核心日活跃用户数(DAU)")
                .indicatorACode("core_dau_cnt")
                .indicatorADomain("增长域")
                .indicatorBId("ind-004")
                .indicatorBName("登录日活跃用户数(DAU)")
                .indicatorBCode("login_dau_cnt")
                .indicatorBDomain("渠道域")
                .conflictType(diff2.getConflictType())
                .similarityScore(new BigDecimal("0.78"))
                .formulaA(f2A)
                .formulaB(f2B)
                .astDiffSummary(diff2.getAstSummary() + " | " + diff2.getWhereClauseDiff())
                .status(ConflictStatus.UNRESOLVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        conflictGateway.batchSave(detected);
        return "METRIC_SCAN_" + System.currentTimeMillis();
    }

    public Map<String, Object> getConflictDiff(String conflictId) {
        MetricConflictRecord conflict = conflictGateway.findById(conflictId)
                .orElseThrow(() -> new IllegalArgumentException("冲突事件不存在: " + conflictId));

        MetricAstDiff diff = astComparator.compareFormulas(conflict.getFormulaA(), conflict.getFormulaB());

        Map<String, Object> res = new HashMap<>();
        res.put("conflict", conflict);

        Map<String, Object> indA = new HashMap<>();
        indA.put("id", conflict.getIndicatorAId());
        indA.put("name", conflict.getIndicatorAName());
        indA.put("code", conflict.getIndicatorACode());
        indA.put("domain", conflict.getIndicatorADomain());
        indA.put("formula", conflict.getFormulaA());
        indA.put("whereClause", "status in (1, 2) and pay_time >= today()");
        indA.put("timeWindow", "实时/当天");
        indA.put("relatedAssetCount", 6);

        Map<String, Object> indB = new HashMap<>();
        indB.put("id", conflict.getIndicatorBId());
        indB.put("name", conflict.getIndicatorBName());
        indB.put("code", conflict.getIndicatorBCode());
        indB.put("domain", conflict.getIndicatorBDomain());
        indB.put("formula", conflict.getFormulaB());
        indB.put("whereClause", "status = 1 and pay_time >= today()");
        indB.put("timeWindow", "实时/当天");
        indB.put("relatedAssetCount", 3);

        res.put("indicatorA", indA);
        res.put("indicatorB", indB);
        res.put("astDiff", diff);
        return res;
    }

    @Transactional(rollbackFor = Exception.class)
    public void reconcileConflict(String conflictId, String canonicalIndicatorId, String strategy, String comment, String operator) {
        MetricConflictRecord conflict = conflictGateway.findById(conflictId)
                .orElseThrow(() -> new IllegalArgumentException("冲突事件不存在: " + conflictId));

        String aliasId = canonicalIndicatorId.equals(conflict.getIndicatorAId()) ? conflict.getIndicatorBId() : conflict.getIndicatorAId();

        conflict.reconcile(canonicalIndicatorId, operator != null ? operator : "gov_admin", comment != null ? comment : "一键标准化对齐归并");
        conflictGateway.update(conflict);

        MetricReconciliationLog reconLog = MetricReconciliationLog.builder()
                .id(UUID.randomUUID().toString())
                .conflictId(conflictId)
                .canonicalId(canonicalIndicatorId)
                .aliasId(aliasId)
                .migratedAssetCount(3)
                .operator(operator != null ? operator : "gov_admin")
                .reconcileStrategy(strategy != null ? strategy : "MERGE_TO_ALIAS")
                .createdAt(LocalDateTime.now())
                .build();

        reconciliationGateway.save(reconLog);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markSuspect(String conflictId, String reason, String operator) {
        MetricConflictRecord conflict = conflictGateway.findById(conflictId)
                .orElseThrow(() -> new IllegalArgumentException("冲突事件不存在: " + conflictId));

        conflict.markSuspect(operator != null ? operator : "gov_admin", reason);
        conflictGateway.update(conflict);
    }

    @Transactional(rollbackFor = Exception.class)
    public void dismissConflict(String conflictId, String reason, String operator) {
        MetricConflictRecord conflict = conflictGateway.findById(conflictId)
                .orElseThrow(() -> new IllegalArgumentException("冲突事件不存在: " + conflictId));

        conflict.dismiss(operator != null ? operator : "gov_admin", reason);
        conflictGateway.update(conflict);
    }
}
