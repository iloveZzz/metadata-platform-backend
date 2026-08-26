package com.yss.datasecurity.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yss.datasecurity.domain.gateway.SecurityGradeGateway;
import com.yss.datasecurity.domain.model.SecurityGrade;
import com.yss.datasecurity.infrastructure.convertor.SecurityGradePOConvertor;
import com.yss.datasecurity.repository.entity.DataCategoryPO;
import com.yss.datasecurity.repository.entity.SecurityGradePO;
import com.yss.datasecurity.repository.entity.SensitiveTaggingRecordPO;
import com.yss.datasecurity.repository.mapper.DataCategoryRepository;
import com.yss.datasecurity.repository.mapper.SecurityGradeRepository;
import com.yss.datasecurity.repository.mapper.SensitiveTaggingRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SecurityGradeGatewayImpl implements SecurityGradeGateway {

    private final SecurityGradeRepository securityGradeRepository;
    private final DataCategoryRepository dataCategoryRepository;
    private final SensitiveTaggingRecordRepository sensitiveTaggingRecordRepository;
    private final SecurityGradePOConvertor convertor;

    @Override
    public List<SecurityGrade> listAll() {
        LambdaQueryWrapper<SecurityGradePO> query = new LambdaQueryWrapper<SecurityGradePO>()
            .orderByDesc(SecurityGradePO::getSensitivityScore);
        List<SecurityGradePO> pos = securityGradeRepository.selectList(query);
        List<SecurityGrade> list = convertor.toDomainList(pos);
        for (SecurityGrade g : list) {
            g.setBoundCategoriesCount(countBoundCategories(g.getId()));
            g.setReferencedRulesCount(countReferencedRules(g.getId()));
            g.setActiveFieldsCount(0);
        }
        return list;
    }

    @Override
    public Optional<SecurityGrade> findById(Long id) {
        SecurityGradePO po = securityGradeRepository.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        SecurityGrade grade = convertor.toDomain(po);
        grade.setBoundCategoriesCount(countBoundCategories(id));
        grade.setReferencedRulesCount(countReferencedRules(id));
        grade.setActiveFieldsCount(0);
        return Optional.of(grade);
    }

    @Override
    public Optional<SecurityGrade> findByName(String gradeName) {
        LambdaQueryWrapper<SecurityGradePO> query = new LambdaQueryWrapper<SecurityGradePO>()
            .eq(SecurityGradePO::getGradeName, gradeName);
        SecurityGradePO po = securityGradeRepository.selectOne(query);
        return Optional.ofNullable(convertor.toDomain(po));
    }

    @Override
    public Optional<SecurityGrade> findByCode(String gradeCode) {
        LambdaQueryWrapper<SecurityGradePO> query = new LambdaQueryWrapper<SecurityGradePO>()
            .eq(SecurityGradePO::getGradeCode, gradeCode);
        SecurityGradePO po = securityGradeRepository.selectOne(query);
        return Optional.ofNullable(convertor.toDomain(po));
    }

    @Override
    public SecurityGrade save(SecurityGrade securityGrade) {
        SecurityGradePO po = convertor.toPO(securityGrade);
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
        securityGradeRepository.insert(po);
        return convertor.toDomain(po);
    }

    @Override
    public void update(SecurityGrade securityGrade) {
        SecurityGradePO po = convertor.toPO(securityGrade);
        po.setUpdatedAt(LocalDateTime.now());
        securityGradeRepository.updateById(po);
    }

    @Override
    public void deleteById(Long id) {
        securityGradeRepository.deleteById(id);
    }

    @Override
    public int countBoundCategories(Long gradeId) {
        LambdaQueryWrapper<DataCategoryPO> query = new LambdaQueryWrapper<DataCategoryPO>()
            .eq(DataCategoryPO::getSecurityGradeId, gradeId);
        Long count = dataCategoryRepository.selectCount(query);
        return count == null ? 0 : count.intValue();
    }

    @Override
    public int countReferencedRules(Long gradeId) {
        LambdaQueryWrapper<SensitiveTaggingRecordPO> query = new LambdaQueryWrapper<SensitiveTaggingRecordPO>()
            .eq(SensitiveTaggingRecordPO::getSecurityGradeId, gradeId);
        Long count = sensitiveTaggingRecordRepository.selectCount(query);
        return count == null ? 0 : count.intValue();
    }
}
