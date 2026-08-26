package com.yss.datasecurity.domain.gateway;

import com.yss.datasecurity.domain.model.MaskingRule;

import java.util.List;
import java.util.Optional;

public interface MaskingRuleGateway {
    List<MaskingRule> pageRules(int pageIndex, int pageSize, String keyword, String ruleType, Long categoryId, String applyScene);
    long countRules(String keyword, String ruleType, Long categoryId, String applyScene);
    Optional<MaskingRule> findById(Long id);
    Optional<MaskingRule> findByCategoryId(Long categoryId);
    MaskingRule save(MaskingRule rule);
    MaskingRule update(MaskingRule rule);
    void deleteById(Long id);
    List<MaskingRule> listActiveRules();
}
