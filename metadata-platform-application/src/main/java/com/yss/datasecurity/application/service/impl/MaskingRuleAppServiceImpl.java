package com.yss.datasecurity.application.service.impl;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.convertor.MaskingRuleConvertor;
import com.yss.datasecurity.application.dto.MaskEvaluationResponseVO;
import com.yss.datasecurity.application.dto.MaskQueryEvaluationRequestDTO;
import com.yss.datasecurity.application.dto.MaskingRuleCreateDTO;
import com.yss.datasecurity.application.dto.MaskingRuleUpdateDTO;
import com.yss.datasecurity.application.dto.MaskingRuleVO;
import com.yss.datasecurity.application.service.MaskingRuleAppService;
import com.yss.datasecurity.domain.exception.DataSecurityException;
import com.yss.datasecurity.domain.gateway.MaskingRuleGateway;
import com.yss.datasecurity.domain.model.MaskingRule;
import com.yss.datasecurity.domain.service.MaskingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaskingRuleAppServiceImpl implements MaskingRuleAppService {

    private final MaskingRuleGateway maskingRuleGateway;
    private final MaskingEngine maskingEngine;
    private final MaskingRuleConvertor convertor;

    @Override
    public PageResult<MaskingRuleVO> pageRules(int pageIndex, int pageSize, String keyword, String ruleType, Long categoryId, String applyScene) {
        List<MaskingRule> list = maskingRuleGateway.pageRules(pageIndex, pageSize, keyword, ruleType, categoryId, applyScene);
        long total = maskingRuleGateway.countRules(keyword, ruleType, categoryId, applyScene);
        List<MaskingRuleVO> voList = convertor.toVOList(list);
        return PageResult.of(voList, (int) total, pageSize, pageIndex);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRule(MaskingRuleCreateDTO dto) {
        MaskingRule domain = convertor.toDomain(dto);
        if (domain.getStatus() == null || domain.getStatus().isEmpty()) {
            domain.setStatus("ENABLED");
        }
        if (domain.getOwner() == null || domain.getOwner().isEmpty()) {
            domain.setOwner("安全管理员");
        }
        MaskingRule saved = maskingRuleGateway.save(domain);
        return saved.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRule(Long id, MaskingRuleUpdateDTO dto) {
        MaskingRule existing = maskingRuleGateway.findById(id)
            .orElseThrow(() -> new DataSecurityException("RULE_NOT_FOUND", "脱敏规则不存在: " + id));

        MaskingRule domain = convertor.toDomain(dto);
        domain.setId(id);
        if (domain.getRuleName() == null || domain.getRuleName().trim().isEmpty()) {
            domain.setRuleName(existing.getRuleName()); // 规则名称不支持修改，保持原名称
        }
        if (domain.getOwner() == null) {
            domain.setOwner(existing.getOwner());
        }
        if (domain.getStatus() == null) {
            domain.setStatus(existing.getStatus());
        }
        maskingRuleGateway.update(domain);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id) {
        maskingRuleGateway.findById(id)
            .orElseThrow(() -> new DataSecurityException("RULE_NOT_FOUND", "脱敏规则不存在: " + id));
        maskingRuleGateway.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status) {
        MaskingRule existing = maskingRuleGateway.findById(id)
            .orElseThrow(() -> new DataSecurityException("RULE_NOT_FOUND", "脱敏规则不存在: " + id));
        existing.setStatus(status);
        maskingRuleGateway.update(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferOwner(com.yss.datasecurity.application.dto.MaskingRuleTransferOwnerDTO dto) {
        if (dto.getRuleIds() == null || dto.getRuleIds().isEmpty()) {
            return;
        }
        for (Long id : dto.getRuleIds()) {
            maskingRuleGateway.findById(id).ifPresent(rule -> {
                rule.setOwner(dto.getNewOwner());
                maskingRuleGateway.update(rule);
            });
        }
    }

    private static final java.util.concurrent.atomic.AtomicReference<com.yss.datasecurity.application.dto.DefaultMaskingPolicyVO> DEFAULT_POLICY_REF =
        new java.util.concurrent.atomic.AtomicReference<>(
            com.yss.datasecurity.application.dto.DefaultMaskingPolicyVO.builder()
                .id(1L)
                .securityGrade("L3")
                .algorithmType("MASK_FIXED_STAR")
                .description("默认托底敏感数据保护策略：未单独配置脱敏规则的机密及以上数据自动采用定长掩码***脱敏")
                .updatedAt(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build()
        );

    @Override
    public com.yss.datasecurity.application.dto.DefaultMaskingPolicyVO getDefaultPolicy() {
        return DEFAULT_POLICY_REF.get();
    }

    @Override
    public void saveDefaultPolicy(com.yss.datasecurity.application.dto.DefaultMaskingPolicyDTO dto) {
        DEFAULT_POLICY_REF.set(
            com.yss.datasecurity.application.dto.DefaultMaskingPolicyVO.builder()
                .id(1L)
                .securityGrade(dto.getSecurityGrade())
                .algorithmType(dto.getAlgorithmType())
                .description(dto.getDescription())
                .updatedAt(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build()
        );
    }

    @Override
    public MaskEvaluationResponseVO evaluateMaskQuery(MaskQueryEvaluationRequestDTO dto) {
        List<MaskingRule> activeRules = maskingRuleGateway.listActiveRules();
        List<Map<String, Object>> maskedRows = new ArrayList<>();
        int appliedRulesCount = 0;

        for (Map<String, Object> rawRow : dto.getRawRows()) {
            Map<String, Object> maskedRow = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : rawRow.entrySet()) {
                String col = entry.getKey();
                Object rawVal = entry.getValue();

                // 查找匹配分类的脱敏规则
                MaskingRule matchedRule = null;
                for (MaskingRule rule : activeRules) {
                    if (isColumnMatchedRule(col, rule)) {
                        matchedRule = rule;
                        break;
                    }
                }

                if (matchedRule != null) {
                    maskedRow.put(col, maskingEngine.maskValue(rawVal, matchedRule));
                    appliedRulesCount++;
                } else if (isSensitiveColumnByDefault(col)) {
                    // L3+ 敏感字段默认托底遮盖
                    maskedRow.put(col, maskingEngine.applyDefaultFallback(rawVal));
                    appliedRulesCount++;
                } else {
                    maskedRow.put(col, rawVal);
                }
            }
            maskedRows.add(maskedRow);
        }

        return MaskEvaluationResponseVO.builder()
            .whitelisted(false)
            .appliedRulesCount(appliedRulesCount)
            .maskedRows(maskedRows)
            .build();
    }

    private boolean isColumnMatchedRule(String col, MaskingRule rule) {
        if (rule.getRuleName() != null && col.toLowerCase().contains(rule.getRuleName().toLowerCase())) {
            return true;
        }
        if (rule.getCategoryName() != null && col.toLowerCase().contains(rule.getCategoryName().toLowerCase())) {
            return true;
        }
        return false;
    }

    private boolean isSensitiveColumnByDefault(String col) {
        String lower = col.toLowerCase();
        return lower.contains("phone") || lower.contains("mobile") || lower.contains("cell")
            || lower.contains("id_card") || lower.contains("idcard") || lower.contains("cert_no")
            || lower.contains("card_no") || lower.contains("bank_card") || lower.contains("salary")
            || lower.contains("password") || lower.contains("secret");
    }
}
