package com.yss.datasecurity.domain.gateway;

import com.yss.datasecurity.domain.model.RecognitionRule;

import java.util.List;
import java.util.Optional;

public interface RecognitionRuleGateway {
    List<RecognitionRule> pageRules(int pageIndex, int pageSize, String keyword, Long categoryId, String owner, Boolean onlyMine, String currentUsername);
    long countRules(String keyword, Long categoryId, String owner, Boolean onlyMine, String currentUsername);
    Optional<RecognitionRule> findById(Long id);
    Optional<RecognitionRule> findByName(String ruleName);
    RecognitionRule save(RecognitionRule rule);
    void update(RecognitionRule rule);
    void deleteById(Long id);
    void updateStatus(Long id, String status);
    void updateOwner(Long id, String newOwner);
    void clearTaggedFields(Long ruleId);
    List<RecognitionRule> listAllActiveRules();
}
