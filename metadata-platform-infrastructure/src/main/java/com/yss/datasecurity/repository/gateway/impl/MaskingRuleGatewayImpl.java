package com.yss.datasecurity.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.datasecurity.domain.gateway.MaskingRuleGateway;
import com.yss.datasecurity.domain.model.MaskingRule;
import com.yss.datasecurity.infrastructure.convertor.MaskingRulePOConvertor;
import com.yss.datasecurity.repository.entity.MaskingRulePO;
import com.yss.datasecurity.repository.mapper.MaskingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MaskingRuleGatewayImpl implements MaskingRuleGateway {

    private final MaskingRuleRepository repository;
    private final MaskingRulePOConvertor convertor;

    @Override
    public List<MaskingRule> pageRules(int pageIndex, int pageSize, String keyword, String ruleType, Long categoryId, String applyScene) {
        Page<MaskingRulePO> page = new Page<>(pageIndex, pageSize);
        LambdaQueryWrapper<MaskingRulePO> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            query.and(q -> q.like(MaskingRulePO::getRuleName, kw)
                    .or().like(MaskingRulePO::getDescription, kw));
        }
        if (ruleType != null && !ruleType.trim().isEmpty()) {
            query.eq(MaskingRulePO::getAlgorithmType, ruleType.trim());
        }
        if (categoryId != null) {
            query.eq(MaskingRulePO::getCategoryId, categoryId);
        }
        if (applyScene != null && !applyScene.trim().isEmpty()) {
            query.like(MaskingRulePO::getApplyScene, applyScene.trim());
        }
        query.orderByDesc(MaskingRulePO::getCreatedAt);

        Page<MaskingRulePO> result = repository.selectPage(page, query);
        return convertor.toDomainList(result.getRecords());
    }

    @Override
    public long countRules(String keyword, String ruleType, Long categoryId, String applyScene) {
        LambdaQueryWrapper<MaskingRulePO> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            query.and(q -> q.like(MaskingRulePO::getRuleName, kw)
                    .or().like(MaskingRulePO::getDescription, kw));
        }
        if (ruleType != null && !ruleType.trim().isEmpty()) {
            query.eq(MaskingRulePO::getAlgorithmType, ruleType.trim());
        }
        if (categoryId != null) {
            query.eq(MaskingRulePO::getCategoryId, categoryId);
        }
        if (applyScene != null && !applyScene.trim().isEmpty()) {
            query.like(MaskingRulePO::getApplyScene, applyScene.trim());
        }
        Long count = repository.selectCount(query);
        return count == null ? 0 : count;
    }

    @Override
    public Optional<MaskingRule> findById(Long id) {
        MaskingRulePO po = repository.selectById(id);
        return Optional.ofNullable(convertor.toDomain(po));
    }

    @Override
    public Optional<MaskingRule> findByCategoryId(Long categoryId) {
        LambdaQueryWrapper<MaskingRulePO> query = new LambdaQueryWrapper<MaskingRulePO>()
            .eq(MaskingRulePO::getCategoryId, categoryId)
            .in(MaskingRulePO::getStatus, "ACTIVE", "ENABLED");
        MaskingRulePO po = repository.selectOne(query);
        return Optional.ofNullable(convertor.toDomain(po));
    }

    @Override
    public MaskingRule save(MaskingRule rule) {
        MaskingRulePO po = convertor.toPO(rule);
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
        repository.insert(po);
        return convertor.toDomain(po);
    }

    @Override
    public MaskingRule update(MaskingRule rule) {
        MaskingRulePO po = convertor.toPO(rule);
        po.setUpdatedAt(LocalDateTime.now());
        repository.updateById(po);
        return convertor.toDomain(po);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public List<MaskingRule> listActiveRules() {
        LambdaQueryWrapper<MaskingRulePO> query = new LambdaQueryWrapper<MaskingRulePO>()
            .in(MaskingRulePO::getStatus, "ACTIVE", "ENABLED");
        List<MaskingRulePO> list = repository.selectList(query);
        return convertor.toDomainList(list);
    }
}
