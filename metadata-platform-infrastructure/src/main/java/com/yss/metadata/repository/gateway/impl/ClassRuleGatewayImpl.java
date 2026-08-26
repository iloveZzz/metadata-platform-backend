package com.yss.metadata.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.metadata.domain.governance.gateway.ClassRuleGateway;
import com.yss.metadata.domain.governance.model.ClassRule;
import com.yss.metadata.repository.ClassRuleRepository;
import com.yss.metadata.infrastructure.convertor.ClassRuleConvertor;
import com.yss.metadata.repository.entity.ClassRulePO;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 分类规则仓储实现（MyBatis-Plus；class_rule 表）。
 */
@Repository
public class ClassRuleGatewayImpl implements ClassRuleGateway {

    private final ClassRuleRepository classRuleRepository;
    private final ClassRuleConvertor classRuleConvertor;

    @Autowired
    public ClassRuleGatewayImpl(ClassRuleRepository classRuleRepository) {
        this(classRuleRepository, Mappers.getMapper(ClassRuleConvertor.class));
    }

    public ClassRuleGatewayImpl(ClassRuleRepository classRuleRepository, ClassRuleConvertor classRuleConvertor) {
        this.classRuleRepository = classRuleRepository;
        this.classRuleConvertor = classRuleConvertor != null ? classRuleConvertor : Mappers.getMapper(ClassRuleConvertor.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassRule> findAll() {
        return classRuleConvertor.toDomainList(
                classRuleRepository.selectList(Wrappers.<ClassRulePO>lambdaQuery().orderByAsc(ClassRulePO::getId)));
    }
    @Override
    @Transactional(readOnly = true)
    public List<ClassRule> findEnabled() {
        return classRuleConvertor.toDomainList(
                classRuleRepository.selectList(Wrappers.<ClassRulePO>lambdaQuery()
                        .eq(ClassRulePO::getEnabled, Boolean.TRUE)
                        .orderByAsc(ClassRulePO::getId)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClassRule> findById(String id) {
        ClassRulePO po = classRuleRepository.selectById(id);
        return po == null ? Optional.empty() : Optional.of(classRuleConvertor.toDomain(po));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassRule save(ClassRule rule) {
        ClassRulePO po = classRuleConvertor.toPO(rule);
        if (classRuleRepository.selectById(po.getId()) != null) {
            classRuleRepository.updateById(po);
        } else {
            classRuleRepository.insert(po);
        }
        return classRuleConvertor.toDomain(po);
    }
}
