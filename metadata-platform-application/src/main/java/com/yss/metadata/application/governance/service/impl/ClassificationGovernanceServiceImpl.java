package com.yss.metadata.application.governance.service.impl;

import com.yss.metadata.application.governance.service.ClassificationGovernanceService;
import com.yss.metadata.application.governance.service.convertor.GovernanceAppConvertor;
import com.yss.metadata.client.dto.cmd.ClassRuleCmd;
import com.yss.metadata.client.vo.ClassRuleVO;
import com.yss.metadata.client.vo.ClassificationOverviewVO;
import com.yss.metadata.client.vo.ClassificationVO;
import com.yss.metadata.client.vo.PropagateTaskVO;
import com.yss.metadata.domain.asset.gateway.AssetRepository;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.audit.gateway.AuditLogGateway;
import com.yss.metadata.domain.audit.model.AuditLogEntry;
import com.yss.metadata.domain.governance.exception.ClassificationNotFoundException;
import com.yss.metadata.domain.governance.exception.ClassRuleNotFoundException;
import com.yss.metadata.domain.governance.gateway.ClassRuleGateway;
import com.yss.metadata.domain.governance.gateway.ClassificationGateway;
import com.yss.metadata.domain.governance.gateway.PropagateTaskGateway;
import com.yss.metadata.domain.governance.model.ClassRule;
import com.yss.metadata.domain.governance.model.Classification;
import com.yss.metadata.domain.governance.model.PropagateTask;
import com.yss.metadata.domain.governance.model.PropagateTaskStatus;
import com.yss.metadata.domain.lineage.gateway.ImpactAnalysisRepository;
import com.yss.metadata.domain.lineage.model.ImpactNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * 分级分类治理应用服务实现（WU-04-01~04）。
 *
 * <p>规则：创建/列表/启停（幂等）+ 审计（classify.rule / classify.rule.status）；
 * 结果：候选确认（幂等）/ 修正（correctedName 覆盖流转已修正）；
 * 传播：沿血缘边下游全量召回（复用影响分析递归 CTE，深度上限对齐
 * {@link #MAX_PROPAGATE_DEPTH}）→ 目标分类写入下游资产 asset.classification
 * （仅覆盖空分类，不降级不覆盖人工已设）→ coverage=受影响资产数（可核验）→
 * 同 classification+version 只跑一次幂等（propagate_task）+ 审计（classify.propagate）。</p>
 *
 * <p>受控解读（代码注释登记）：列级分类沿血缘传播为资产级（lineage_edge 无列级边，
 * 列级传播 seam-deferred）；覆盖范围以受影响资产数承载（可经资产详情核验）。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClassificationGovernanceServiceImpl implements ClassificationGovernanceService {

    /** 传播深度上限（环保护兜底；对齐影响分析深度上限） */
    public static final int MAX_PROPAGATE_DEPTH = 10;

    /** 审计动作 */
    private static final String AUDIT_ACTION_RULE = "classify.rule";
    private static final String AUDIT_ACTION_RULE_STATUS = "classify.rule.status";
    private static final String AUDIT_ACTION_PROPAGATE = "classify.propagate";

    private final ClassRuleGateway classRuleGateway;
    private final ClassificationGateway classificationGateway;
    private final PropagateTaskGateway propagateTaskGateway;
    private final ImpactAnalysisRepository impactAnalysisRepository;
    private final AssetRepository assetRepository;
    private final AuditLogGateway auditLogRepository;
    private final GovernanceAppConvertor governanceAppConvertor;

    @Override
    @Transactional(readOnly = true)
    public ClassificationOverviewVO getOverview() {
        ClassificationOverviewVO vo = governanceAppConvertor.toOverviewVO(
                classRuleGateway.findAll(), classificationGateway.findAll());
        // 组合字段填充（F2 复审修复）：assetName 经资产仓储解析；columnName 经列清单匹配
        for (ClassificationVO result : vo.getResults()) {
            enrichDisplayFields(result);
        }
        return vo;
    }

    /**
     * 填充展示组合字段（assetName / columnName；供结果区表格展示）。
     */
    private void enrichDisplayFields(ClassificationVO vo) {
        if (vo.getAssetId() == null || vo.getAssetId().trim().isEmpty()) {
            return;
        }
        assetRepository.findById(vo.getAssetId())
                .ifPresent(asset -> vo.setAssetName(asset.getName()));
        if (vo.getColumnId() != null && !vo.getColumnId().trim().isEmpty()) {
            assetRepository.findColumns(vo.getAssetId()).stream()
                    .filter(column -> vo.getColumnId().equals(column.getId()))
                    .findFirst()
                    .ifPresent(column -> vo.setColumnName(column.getName()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassRuleVO createRule(ClassRuleCmd cmd, String operator) {
        ClassRule rule = ClassRule.builder()
                .id(UUID.randomUUID().toString())
                .name(cmd.getName())
                .type(cmd.getType())
                .pattern(cmd.getPattern())
                .enabled(cmd.getEnabled() == null || cmd.getEnabled())
                .build();
        ClassRule saved = classRuleGateway.save(rule);
        auditLogRepository.record(AuditLogEntry.builder()
                .id(UUID.randomUUID().toString())
                .operator(operator)
                .action(AUDIT_ACTION_RULE)
                .object(saved.getId())
                .result("success")
                .time(LocalDateTime.now())
                .build());
        log.info("分类规则已创建/修正，ruleId={}, name={}, operator={}",
                saved.getId(), saved.getName(), operator);
        return governanceAppConvertor.toRuleVO(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassRuleVO toggleRule(String id, boolean enabled, String operator) {
        ClassRule rule = classRuleGateway.findById(id)
                .orElseThrow(() -> new ClassRuleNotFoundException(id));
        rule.setEnabled(enabled);
        ClassRule saved = classRuleGateway.save(rule);
        auditLogRepository.record(AuditLogEntry.builder()
                .id(UUID.randomUUID().toString())
                .operator(operator)
                .action(AUDIT_ACTION_RULE_STATUS)
                .object(saved.getId())
                .result(enabled ? "enabled" : "disabled")
                .time(LocalDateTime.now())
                .build());
        log.info("分类规则启停，ruleId={}, enabled={}, operator={}", saved.getId(), enabled, operator);
        return governanceAppConvertor.toRuleVO(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassificationVO confirm(String id, String correctedName) {
        Classification classification = classificationGateway.findById(id)
                .orElseThrow(() -> new ClassificationNotFoundException(id));
        // 空白/缺省 correctedName 视为确认（契约语义「非空=修正」；空白修正名不报错不提示，F12 注释登记）
        if (correctedName != null && !correctedName.trim().isEmpty()) {
            classification.correct(correctedName);
        } else {
            classification.confirm();
        }
        Classification saved = classificationGateway.save(classification);
        log.info("候选分类确认/修正，id={}, name={}, status={}",
                saved.getId(), saved.getName(), saved.getStatus());
        return governanceAppConvertor.toClassificationVO(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PropagateTaskVO propagate(String id, String operator) {
        Classification classification = classificationGateway.findById(id)
                .orElseThrow(() -> new ClassificationNotFoundException(id));
        String sourceAssetId = classificationGateway.resolveSourceAssetId(classification)
                .orElseThrow(() -> new IllegalArgumentException("分类未关联资产，无法传播"));
        // 传播版本：分类内容签名（同版本只跑一次幂等键；内容修正后版本变化可重新传播）
        String version = buildVersion(classification);

        // 幂等：同 classification+version 进行中/已成功任务复用（合同「进行中任务复用」），
        // failed 允许以同版本重试（传播重试路径）；重试原地复用同一任务行，保持同键单行
        // （F1/F9 复审修复：避免重复行导致幂等失效）
        Optional<PropagateTask> existing = propagateTaskGateway.findByClassificationAndVersion(id, version);
        if (existing.isPresent() && existing.get().getStatus() != PropagateTaskStatus.FAILED) {
            log.info("传播任务幂等复用，taskId={}, classificationId={}, version={}, operator={}",
                    existing.get().getId(), id, version, operator);
            return governanceAppConvertor.toPropagateTaskVO(existing.get());
        }

        PropagateTask task;
        if (existing.isPresent()) {
            // failed 重试：原地复用任务行（重置 pending 重新执行，同键单行不变）
            task = existing.get();
            task.setStatus(PropagateTaskStatus.PENDING);
            task.setCoverage(null);
            task.setFinishedAt(null);
            task.setOperator(operator);
            log.info("传播任务失败重试（原地复用），taskId={}, classificationId={}, version={}, operator={}",
                    task.getId(), id, version, operator);
        } else {
            task = PropagateTask.builder()
                    .id(UUID.randomUUID().toString())
                    .classificationId(id)
                    .version(version)
                    .status(PropagateTaskStatus.PENDING)
                    .operator(operator)
                    .createdAt(LocalDateTime.now())
                    .build();
            log.info("传播任务已创建，taskId={}, classificationId={}, version={}, operator={}",
                    task.getId(), id, version, operator);
        }
        propagateTaskGateway.save(task);

        PropagateTask executed = execute(task, sourceAssetId, classification.getName());
        PropagateTask finalTask = propagateTaskGateway.save(executed);

        auditLogRepository.record(AuditLogEntry.builder()
                .id(UUID.randomUUID().toString())
                .operator(operator)
                .action(AUDIT_ACTION_PROPAGATE)
                .object(finalTask.getId())
                .result(finalTask.getStatus().getValue())
                .time(LocalDateTime.now())
                .build());
        log.info("传播审计已记录，taskId={}, status={}", finalTask.getId(), finalTask.getStatus().getValue());
        return governanceAppConvertor.toPropagateTaskVO(finalTask);
    }

    /**
     * 执行传播：running → 下游全量召回 → 目标分类写入下游资产（仅覆盖空分类）→ success；
     * 失败 → failed（不抛异常，任务状态承载）。coverage=受影响资产数（可核验）。
     *
     * <p>受控偏离登记（F3 复审记录）：失败路径下已写入的下游资产分类随同一事务提交
     * （部分写入，coverage 反映已完成数），任务标记 failed；回滚/补偿语义在切片 05
     * 异步化重估时一并处理。</p>
     */
    private PropagateTask execute(PropagateTask task, String sourceAssetId, String classificationName) {
        task.setStatus(PropagateTaskStatus.RUNNING);
        int updated = 0;
        try {
            propagateTaskGateway.save(task);
            List<ImpactNode> downstream = impactAnalysisRepository.findDownstream(sourceAssetId, MAX_PROPAGATE_DEPTH);
            for (ImpactNode node : downstream) {
                if (applyClassification(node.getAssetId(), classificationName)) {
                    updated++;
                }
            }
            // 源资产自身分类为空时一并设置（列级分类 → 所属资产兜底）
            if (applyClassification(sourceAssetId, classificationName)) {
                updated++;
            }
            task.setStatus(PropagateTaskStatus.SUCCESS);
        } catch (Exception e) {
            log.error("分类传播失败，taskId={}, classificationId={}",
                    task.getId(), task.getClassificationId(), e);
            task.setStatus(PropagateTaskStatus.FAILED);
        }
        task.setCoverage(String.valueOf(updated));
        task.setFinishedAt(LocalDateTime.now());
        return task;
    }

    /**
     * 为目标资产写入分类（仅当资产存在且分类为空；不降级不覆盖人工已设）。
     *
     * @return 是否实际更新（计入覆盖范围）
     */
    private boolean applyClassification(String assetId, String classificationName) {
        if (classificationName == null || classificationName.trim().isEmpty()) {
            return false;
        }
        Optional<Asset> optional = assetRepository.findById(assetId);
        if (!optional.isPresent()) {
            return false;
        }
        Asset asset = optional.get();
        if (asset.getClassification() != null && !asset.getClassification().trim().isEmpty()) {
            return false;
        }
        asset.setClassification(classificationName.trim());
        asset.setUpdatedAt(LocalDateTime.now());
        assetRepository.save(asset);
        return true;
    }

    /**
     * 传播版本 = 分类内容签名（id + name + level + status；修正后版本变化可重新传播）。
     */
    private String buildVersion(Classification classification) {
        return classification.getId() + "#"
                + safe(classification.getName()) + "|"
                + safe(classification.getLevel()) + "|"
                + (classification.getStatus() == null ? "" : classification.getStatus().getValue());
    }

    private String safe(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
