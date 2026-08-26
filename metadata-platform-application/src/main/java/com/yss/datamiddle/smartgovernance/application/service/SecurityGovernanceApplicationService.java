package com.yss.datamiddle.smartgovernance.application.service;

import com.yss.datamiddle.smartgovernance.domain.llm.LlmGateway;
import com.yss.datamiddle.smartgovernance.domain.security.gateway.ClassificationRuleGateway;
import com.yss.datamiddle.smartgovernance.domain.security.gateway.SecurityAuditLogGateway;
import com.yss.datamiddle.smartgovernance.domain.security.gateway.SecurityTemplateGateway;
import com.yss.datamiddle.smartgovernance.domain.security.gateway.SensitiveCandidateGateway;
import com.yss.datamiddle.smartgovernance.domain.security.model.CandidateStatus;
import com.yss.datamiddle.smartgovernance.domain.security.model.ClassificationRule;
import com.yss.datamiddle.smartgovernance.domain.security.model.SecurityAuditLog;
import com.yss.datamiddle.smartgovernance.domain.security.model.SecurityLevel;
import com.yss.datamiddle.smartgovernance.domain.security.model.SecurityTemplate;
import com.yss.datamiddle.smartgovernance.domain.security.model.SensitiveCandidate;
import com.yss.datamiddle.smartgovernance.domain.security.service.ThreeLayerFunnelScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityGovernanceApplicationService {

    private final SecurityTemplateGateway templateGateway;
    private final ClassificationRuleGateway ruleGateway;
    private final SensitiveCandidateGateway candidateGateway;
    private final SecurityAuditLogGateway auditLogGateway;
    private final LlmGateway llmGateway;

    public List<SecurityTemplate> listTemplates(String keyword) {
        return templateGateway.listTemplates(keyword);
    }

    public Optional<SecurityTemplate> getTemplateDetail(String id) {
        Optional<SecurityTemplate> opt = templateGateway.findById(id);
        opt.ifPresent(template -> {
            List<ClassificationRule> rules = ruleGateway.findByTemplateId(template.getId());
            template.setRules(rules);
        });
        return opt;
    }

    @Transactional(rollbackFor = Exception.class)
    public String createTemplate(SecurityTemplate template, List<ClassificationRule> rules) {
        if (template.getId() == null) {
            template.setId(UUID.randomUUID().toString());
        }
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        templateGateway.save(template);

        if (rules != null && !rules.isEmpty()) {
            for (ClassificationRule rule : rules) {
                if (rule.getId() == null) {
                    rule.setId(UUID.randomUUID().toString());
                }
                rule.setTemplateId(template.getId());
                rule.setCreatedAt(LocalDateTime.now());
                rule.setUpdatedAt(LocalDateTime.now());
            }
            ruleGateway.batchSave(rules);
        }
        return template.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(String id, SecurityTemplate updateParam) {
        SecurityTemplate exist = templateGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("合规模板不存在: " + id));

        exist.setTemplateName(updateParam.getTemplateName());
        exist.setDescription(updateParam.getDescription());
        exist.setDefaultAutoApproval(updateParam.getDefaultAutoApproval());
        exist.setDefaultThreshold(updateParam.getDefaultThreshold());
        exist.setIsActive(updateParam.getIsActive());
        exist.setUpdatedAt(LocalDateTime.now());

        templateGateway.update(exist);
    }

    /**
     * 触发异步安全扫描 (模拟执行全域/指定表字段扫描)
     */
    @Transactional(rollbackFor = Exception.class)
    public String triggerScan(String templateId, String dataSource, String databaseName, String tableName) {
        SecurityTemplate template = templateGateway.findById(templateId != null ? templateId : "tpl-jr-0197")
                .orElseGet(() -> {
                    List<SecurityTemplate> all = templateGateway.listTemplates(null);
                    return all.isEmpty() ? null : all.get(0);
                });

        if (template == null) {
            template = SecurityTemplate.builder()
                    .id("tpl-jr-0197")
                    .templateCode("JR_T_0197_2020")
                    .templateName("《金融数据安全分级指南 JR/T 0197-2020》")
                    .defaultAutoApproval(true)
                    .defaultThreshold(new BigDecimal("0.95"))
                    .build();
        }
        List<ClassificationRule> rules = ruleGateway.findByTemplateId(template.getId());
        template.setRules(rules);

        ThreeLayerFunnelScanner scanner = new ThreeLayerFunnelScanner(llmGateway);

        // 模拟识别几条典型业务字段
        List<SensitiveCandidate> candidates = new ArrayList<>();

        // 1. 手机号 (命中正则)
        candidates.add(scanner.scanColumn(template, dataSource != null ? dataSource : "mysql-prod-01",
                databaseName != null ? databaseName : "trade_db", tableName != null ? tableName : "user_t",
                "用户主表", "mobile_phone", "用户绑定手机号码", "VARCHAR(16)", Collections.singletonList("user_id")));

        // 2. 身份证 (非规范缩写，触发 L3 LLM 推理)
        candidates.add(scanner.scanColumn(template, dataSource != null ? dataSource : "mysql-prod-01",
                databaseName != null ? databaseName : "trade_db", tableName != null ? tableName : "cust_info_t",
                "客户信息表", "kh_sfz_no", "客户身份认证主键", "VARCHAR(32)", Collections.singletonList("cust_name")));

        // 3. 银行卡号 (命中正则)
        candidates.add(scanner.scanColumn(template, dataSource != null ? dataSource : "mysql-prod-01",
                databaseName != null ? databaseName : "trade_db", tableName != null ? tableName : "pay_account_t",
                "支付账户表", "bank_card_num", "结算银行卡账号", "VARCHAR(32)", Collections.singletonList("acct_id")));

        candidateGateway.batchSave(candidates);

        // 为自动生效的记录写入审计流水
        List<SecurityAuditLog> autoLogs = new ArrayList<>();
        for (SensitiveCandidate c : candidates) {
            if (c.getStatus() == CandidateStatus.APPROVED) {
                autoLogs.add(SecurityAuditLog.builder()
                        .id(UUID.randomUUID().toString())
                        .candidateId(c.getId())
                        .dataSource(c.getDataSource())
                        .databaseName(c.getDatabaseName())
                        .tableName(c.getTableName())
                        .columnName(c.getColumnName())
                        .previousLevel(null)
                        .newLevel(c.getActualLevel().getCode())
                        .actionType("AUTO_APPROVE")
                        .operator("SYSTEM_AUTO_LLM")
                        .reason("超高置信度(" + c.getConfidence() + ")自动打标生效: " + c.getReasoning())
                        .createdAt(LocalDateTime.now())
                        .build());
            }
        }
        if (!autoLogs.isEmpty()) {
            auditLogGateway.batchSave(autoLogs);
        }

        return "SCAN_TASK_" + System.currentTimeMillis();
    }

    public List<SensitiveCandidate> queryCandidates(
            Integer pageIndex,
            Integer pageSize,
            CandidateStatus status,
            SecurityLevel securityLevel,
            String sensitiveType,
            String keyword
    ) {
        return candidateGateway.queryCandidates(pageIndex, pageSize, status, securityLevel, sensitiveType, keyword);
    }

    public long countCandidates(
            CandidateStatus status,
            SecurityLevel securityLevel,
            String sensitiveType,
            String keyword
    ) {
        return candidateGateway.countCandidates(status, securityLevel, sensitiveType, keyword);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> batchApprove(List<String> candidateIds, String operator) {
        List<SensitiveCandidate> candidates = candidateGateway.findByIds(candidateIds);
        List<SecurityAuditLog> auditLogs = new ArrayList<>();

        for (SensitiveCandidate candidate : candidates) {
            String prev = candidate.getActualLevel() != null ? candidate.getActualLevel().getCode() : null;
            candidate.approve(operator != null ? operator : "sec_admin");

            auditLogs.add(SecurityAuditLog.builder()
                    .id(UUID.randomUUID().toString())
                    .candidateId(candidate.getId())
                    .dataSource(candidate.getDataSource())
                    .databaseName(candidate.getDatabaseName())
                    .tableName(candidate.getTableName())
                    .columnName(candidate.getColumnName())
                    .previousLevel(prev)
                    .newLevel(candidate.getActualLevel().getCode())
                    .actionType("MANUAL_APPROVE")
                    .operator(operator != null ? operator : "sec_admin")
                    .reason("专员人工审核通过采纳: " + candidate.getReasoning())
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        candidateGateway.batchUpdate(candidates);
        auditLogGateway.batchSave(auditLogs);

        Map<String, Integer> res = new HashMap<>();
        res.put("successCount", candidates.size());
        res.put("failureCount", 0);
        return res;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> batchModify(List<String> candidateIds, SecurityLevel targetLevel, String targetSensitiveType, String operator, String reason) {
        List<SensitiveCandidate> candidates = candidateGateway.findByIds(candidateIds);
        List<SecurityAuditLog> auditLogs = new ArrayList<>();

        for (SensitiveCandidate candidate : candidates) {
            String prev = candidate.getActualLevel() != null ? candidate.getActualLevel().getCode() : candidate.getRecommendedLevel().getCode();
            candidate.modify(targetLevel, targetSensitiveType, operator != null ? operator : "sec_admin", reason);

            auditLogs.add(SecurityAuditLog.builder()
                    .id(UUID.randomUUID().toString())
                    .candidateId(candidate.getId())
                    .dataSource(candidate.getDataSource())
                    .databaseName(candidate.getDatabaseName())
                    .tableName(candidate.getTableName())
                    .columnName(candidate.getColumnName())
                    .previousLevel(prev)
                    .newLevel(targetLevel.getCode())
                    .actionType("MANUAL_MODIFY")
                    .operator(operator != null ? operator : "sec_admin")
                    .reason("专员手动修正安全等级: " + reason)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        candidateGateway.batchUpdate(candidates);
        auditLogGateway.batchSave(auditLogs);

        Map<String, Integer> res = new HashMap<>();
        res.put("successCount", candidates.size());
        res.put("failureCount", 0);
        return res;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> batchIgnore(List<String> candidateIds, String operator, String reason) {
        List<SensitiveCandidate> candidates = candidateGateway.findByIds(candidateIds);
        List<SecurityAuditLog> auditLogs = new ArrayList<>();

        for (SensitiveCandidate candidate : candidates) {
            candidate.ignore(operator != null ? operator : "sec_admin", reason);

            auditLogs.add(SecurityAuditLog.builder()
                    .id(UUID.randomUUID().toString())
                    .candidateId(candidate.getId())
                    .dataSource(candidate.getDataSource())
                    .databaseName(candidate.getDatabaseName())
                    .tableName(candidate.getTableName())
                    .columnName(candidate.getColumnName())
                    .previousLevel(candidate.getRecommendedLevel().getCode())
                    .newLevel("L1")
                    .actionType("IGNORE")
                    .operator(operator != null ? operator : "sec_admin")
                    .reason("专员判定为非敏感并忽略: " + reason)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        candidateGateway.batchUpdate(candidates);
        auditLogGateway.batchSave(auditLogs);

        Map<String, Integer> res = new HashMap<>();
        res.put("successCount", candidates.size());
        res.put("failureCount", 0);
        return res;
    }
}
