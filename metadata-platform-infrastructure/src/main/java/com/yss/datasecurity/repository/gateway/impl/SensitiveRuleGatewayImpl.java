package com.yss.datasecurity.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.datasecurity.domain.gateway.SensitiveRuleGateway;
import com.yss.datasecurity.domain.model.SensitiveRule;
import com.yss.datasecurity.infrastructure.convertor.SensitiveRulePOConvertor;
import com.yss.datasecurity.repository.entity.SensitiveRulePO;
import com.yss.datasecurity.repository.mapper.SensitiveRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SensitiveRuleGatewayImpl implements SensitiveRuleGateway {

    private final SensitiveRuleRepository sensitiveRuleRepository;
    private final SensitiveRulePOConvertor convertor;

    @Override
    public List<SensitiveRule> pageRules(int pageIndex, int pageSize, String keyword, String status, String scanScopeType, String ruleType) {
        Page<SensitiveRulePO> page = new Page<>(pageIndex, pageSize);
        LambdaQueryWrapper<SensitiveRulePO> query = buildQuery(keyword, status, scanScopeType, ruleType);
        query.orderByAsc(SensitiveRulePO::getPriority).orderByDesc(SensitiveRulePO::getCreatedAt);

        Page<SensitiveRulePO> result = sensitiveRuleRepository.selectPage(page, query);
        return convertor.toDomainList(result.getRecords());
    }

    @Override
    public long countRules(String keyword, String status, String scanScopeType, String ruleType) {
        LambdaQueryWrapper<SensitiveRulePO> query = buildQuery(keyword, status, scanScopeType, ruleType);
        Long count = sensitiveRuleRepository.selectCount(query);
        return count == null ? 0 : count;
    }

    @Override
    public Optional<SensitiveRule> findById(Long id) {
        SensitiveRulePO po = sensitiveRuleRepository.selectById(id);
        return Optional.ofNullable(convertor.toDomain(po));
    }

    @Override
    public Optional<SensitiveRule> findByName(String ruleName) {
        LambdaQueryWrapper<SensitiveRulePO> query = new LambdaQueryWrapper<SensitiveRulePO>()
            .eq(SensitiveRulePO::getRuleName, ruleName);
        SensitiveRulePO po = sensitiveRuleRepository.selectOne(query);
        return Optional.ofNullable(convertor.toDomain(po));
    }

    @Override
    public SensitiveRule save(SensitiveRule sensitiveRule) {
        SensitiveRulePO po = convertor.toPO(sensitiveRule);
        if (po.getCreatedAt() == null) {
            po.setCreatedAt(LocalDateTime.now());
        }
        if (po.getUpdatedAt() == null) {
            po.setUpdatedAt(LocalDateTime.now());
        }
        if (po.getCreatedBy() == null) {
            po.setCreatedBy("system");
        }
        if (po.getUpdatedBy() == null) {
            po.setUpdatedBy("system");
        }
        sensitiveRuleRepository.insert(po);
        return convertor.toDomain(po);
    }

    @Override
    public void update(SensitiveRule sensitiveRule) {
        SensitiveRulePO po = convertor.toPO(sensitiveRule);
        po.setUpdatedAt(LocalDateTime.now());
        sensitiveRuleRepository.updateById(po);
    }

    @Override
    public void deleteById(Long id) {
        sensitiveRuleRepository.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, String status) {
        SensitiveRulePO po = sensitiveRuleRepository.selectById(id);
        if (po != null) {
            po.setStatus(status);
            po.setUpdatedAt(LocalDateTime.now());
            sensitiveRuleRepository.updateById(po);
        }
    }

    @Override
    public void clearTaggedFields(Long ruleId) {
        SensitiveRulePO po = sensitiveRuleRepository.selectById(ruleId);
        if (po != null) {
            po.setTaggedFieldsCount(0);
            po.setUpdatedAt(LocalDateTime.now());
            sensitiveRuleRepository.updateById(po);
        }
    }

    private LambdaQueryWrapper<SensitiveRulePO> buildQuery(String keyword, String status, String scanScopeType, String ruleType) {
        LambdaQueryWrapper<SensitiveRulePO> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            query.like(SensitiveRulePO::getRuleName, keyword);
        }
        if (status != null && !status.trim().isEmpty()) {
            query.eq(SensitiveRulePO::getStatus, status);
        }
        if (scanScopeType != null && !scanScopeType.trim().isEmpty()) {
            query.eq(SensitiveRulePO::getScanScopeType, scanScopeType);
        }
        if (ruleType != null && !ruleType.trim().isEmpty()) {
            query.eq(SensitiveRulePO::getRuleType, ruleType);
        }
        return query;
    }
}
