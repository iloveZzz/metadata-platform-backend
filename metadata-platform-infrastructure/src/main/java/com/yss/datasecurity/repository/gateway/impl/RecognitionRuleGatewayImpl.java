package com.yss.datasecurity.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.datasecurity.domain.gateway.RecognitionRuleGateway;
import com.yss.datasecurity.domain.model.RecognitionRule;
import com.yss.datasecurity.infrastructure.convertor.RecognitionRulePOConvertor;
import com.yss.datasecurity.repository.entity.RecognitionRulePO;
import com.yss.datasecurity.repository.mapper.RecognitionRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RecognitionRuleGatewayImpl implements RecognitionRuleGateway {

    private final RecognitionRuleRepository repository;
    private final RecognitionRulePOConvertor convertor;

    @Override
    public List<RecognitionRule> pageRules(int pageIndex, int pageSize, String keyword, Long categoryId, String owner, Boolean onlyMine, String currentUsername) {
        Page<RecognitionRulePO> page = new Page<>(pageIndex, pageSize);
        LambdaQueryWrapper<RecognitionRulePO> query = buildQuery(keyword, categoryId, owner, onlyMine, currentUsername);
        query.orderByDesc(RecognitionRulePO::getUpdatedAt).orderByDesc(RecognitionRulePO::getId);

        Page<RecognitionRulePO> result = repository.selectPage(page, query);
        return convertor.toDomainList(result.getRecords());
    }

    @Override
    public long countRules(String keyword, Long categoryId, String owner, Boolean onlyMine, String currentUsername) {
        LambdaQueryWrapper<RecognitionRulePO> query = buildQuery(keyword, categoryId, owner, onlyMine, currentUsername);
        Long count = repository.selectCount(query);
        return count == null ? 0 : count;
    }

    @Override
    public Optional<RecognitionRule> findById(Long id) {
        RecognitionRulePO po = repository.selectById(id);
        return Optional.ofNullable(convertor.toDomain(po));
    }

    @Override
    public Optional<RecognitionRule> findByName(String ruleName) {
        LambdaQueryWrapper<RecognitionRulePO> query = new LambdaQueryWrapper<RecognitionRulePO>()
                .eq(RecognitionRulePO::getRuleName, ruleName);
        RecognitionRulePO po = repository.selectOne(query);
        return Optional.ofNullable(convertor.toDomain(po));
    }

    @Override
    public RecognitionRule save(RecognitionRule rule) {
        RecognitionRulePO po = convertor.toPO(rule);
        if (po.getCreatedAt() == null) {
            po.setCreatedAt(LocalDateTime.now());
        }
        if (po.getUpdatedAt() == null) {
            po.setUpdatedAt(LocalDateTime.now());
        }
        if (po.getCreatedBy() == null) {
            po.setCreatedBy("admin");
        }
        if (po.getUpdatedBy() == null) {
            po.setUpdatedBy("admin");
        }
        if (po.getTaggedFieldsCount() == null) {
            po.setTaggedFieldsCount(0);
        }
        if (po.getLineageInheritanceEnabled() == null) {
            po.setLineageInheritanceEnabled(false);
        }
        repository.insert(po);
        return convertor.toDomain(po);
    }

    @Override
    public void update(RecognitionRule rule) {
        RecognitionRulePO po = convertor.toPO(rule);
        po.setUpdatedAt(LocalDateTime.now());
        repository.updateById(po);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, String status) {
        RecognitionRulePO po = repository.selectById(id);
        if (po != null) {
            po.setStatus(status);
            po.setUpdatedAt(LocalDateTime.now());
            repository.updateById(po);
        }
    }

    @Override
    public void updateOwner(Long id, String newOwner) {
        RecognitionRulePO po = repository.selectById(id);
        if (po != null) {
            po.setOwner(newOwner);
            po.setUpdatedAt(LocalDateTime.now());
            repository.updateById(po);
        }
    }

    @Override
    public void clearTaggedFields(Long ruleId) {
        RecognitionRulePO po = repository.selectById(ruleId);
        if (po != null) {
            po.setTaggedFieldsCount(0);
            po.setUpdatedAt(LocalDateTime.now());
            repository.updateById(po);
        }
    }

    @Override
    public List<RecognitionRule> listAllActiveRules() {
        LambdaQueryWrapper<RecognitionRulePO> query = new LambdaQueryWrapper<RecognitionRulePO>()
                .eq(RecognitionRulePO::getStatus, "ENABLED");
        return convertor.toDomainList(repository.selectList(query));
    }

    private LambdaQueryWrapper<RecognitionRulePO> buildQuery(String keyword, Long categoryId, String owner, Boolean onlyMine, String currentUsername) {
        LambdaQueryWrapper<RecognitionRulePO> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            query.like(RecognitionRulePO::getRuleName, keyword.trim());
        }
        if (owner != null && !owner.trim().isEmpty()) {
            query.eq(RecognitionRulePO::getOwner, owner.trim());
        }
        if (Boolean.TRUE.equals(onlyMine)) {
            String uname = (currentUsername != null && !currentUsername.trim().isEmpty()) ? currentUsername : "admin";
            query.eq(RecognitionRulePO::getOwner, uname);
        }
        if (categoryId != null) {
            query.like(RecognitionRulePO::getCategoryScopeConfig, String.valueOf(categoryId));
        }
        return query;
    }
}
