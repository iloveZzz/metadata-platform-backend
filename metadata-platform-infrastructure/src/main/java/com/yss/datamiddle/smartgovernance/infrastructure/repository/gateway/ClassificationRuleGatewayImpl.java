package com.yss.datamiddle.smartgovernance.infrastructure.repository.gateway;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yss.datamiddle.smartgovernance.domain.security.gateway.ClassificationRuleGateway;
import com.yss.datamiddle.smartgovernance.domain.security.model.ClassificationRule;
import com.yss.datamiddle.smartgovernance.domain.security.model.SecurityLevel;
import com.yss.datamiddle.smartgovernance.infrastructure.repository.mapper.ClassificationRuleMapper;
import com.yss.datamiddle.smartgovernance.infrastructure.repository.po.ClassificationRulePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ClassificationRuleGatewayImpl implements ClassificationRuleGateway {

    private final ClassificationRuleMapper ruleMapper;

    @Override
    public List<ClassificationRule> findByTemplateId(String templateId) {
        LambdaQueryWrapper<ClassificationRulePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassificationRulePO::getTemplateId, templateId)
                .orderByAsc(ClassificationRulePO::getPriority);
        return ruleMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void batchSave(List<ClassificationRule> rules) {
        if (rules != null && !rules.isEmpty()) {
            for (ClassificationRule rule : rules) {
                ruleMapper.insert(toPO(rule));
            }
        }
    }

    private ClassificationRule toDomain(ClassificationRulePO po) {
        if (po == null) return null;
        return ClassificationRule.builder()
                .id(po.getId())
                .templateId(po.getTemplateId())
                .sensitiveType(po.getSensitiveType())
                .sensitiveName(po.getSensitiveName())
                .securityLevel(SecurityLevel.of(po.getSecurityLevel()))
                .clauseRef(po.getClauseRef())
                .regexPattern(po.getRegexPattern())
                .dictionaryWords(po.getDictionaryWords())
                .semanticPrompt(po.getSemanticPrompt())
                .isActive(po.getIsActive() != null && po.getIsActive() == 1)
                .priority(po.getPriority())
                .createdBy(po.getCreatedBy())
                .updatedBy(po.getUpdatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private ClassificationRulePO toPO(ClassificationRule d) {
        if (d == null) return null;
        return ClassificationRulePO.builder()
                .id(d.getId())
                .templateId(d.getTemplateId())
                .sensitiveType(d.getSensitiveType())
                .sensitiveName(d.getSensitiveName())
                .securityLevel(d.getSecurityLevel() != null ? d.getSecurityLevel().getCode() : "L1")
                .clauseRef(d.getClauseRef())
                .regexPattern(d.getRegexPattern())
                .dictionaryWords(d.getDictionaryWords())
                .semanticPrompt(d.getSemanticPrompt())
                .isActive(Boolean.TRUE.equals(d.getIsActive()) ? 1 : 0)
                .priority(d.getPriority() != null ? d.getPriority() : 100)
                .createdBy(d.getCreatedBy())
                .updatedBy(d.getUpdatedBy())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
