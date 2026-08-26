package com.yss.datasecurity.domain.gateway;

import com.yss.datasecurity.domain.model.SensitiveRule;

import java.util.List;
import java.util.Optional;

public interface SensitiveRuleGateway {
    List<SensitiveRule> pageRules(int pageIndex, int pageSize, String keyword, String status, String scanScopeType, String ruleType);
    long countRules(String keyword, String status, String scanScopeType, String ruleType);
    Optional<SensitiveRule> findById(Long id);
    Optional<SensitiveRule> findByName(String ruleName);
    SensitiveRule save(SensitiveRule sensitiveRule);
    void update(SensitiveRule sensitiveRule);
    void deleteById(Long id);
    void updateStatus(Long id, String status);
    void clearTaggedFields(Long ruleId);
}
