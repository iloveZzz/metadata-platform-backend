package com.yss.datasecurity.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.datasecurity.domain.gateway.SensitiveTaggingRecordGateway;
import com.yss.datasecurity.domain.model.SensitiveTaggingRecord;
import com.yss.datasecurity.infrastructure.convertor.SensitiveRecordPOConvertor;
import com.yss.datasecurity.repository.entity.SensitiveTaggingRecordPO;
import com.yss.datasecurity.repository.mapper.SensitiveTaggingRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SensitiveTaggingRecordGatewayImpl implements SensitiveTaggingRecordGateway {

    private final SensitiveTaggingRecordRepository repository;
    private final SensitiveRecordPOConvertor convertor;

    @Override
    public List<SensitiveTaggingRecord> pageRecords(int pageIndex, int pageSize, String keyword, Long categoryId, Long gradeId, Boolean isLocked, String datasourceId) {
        Page<SensitiveTaggingRecordPO> page = new Page<>(pageIndex, pageSize);
        LambdaQueryWrapper<SensitiveTaggingRecordPO> query = buildQuery(keyword, categoryId, gradeId, isLocked, datasourceId);
        query.orderByDesc(SensitiveTaggingRecordPO::getSensitivityScore).orderByDesc(SensitiveTaggingRecordPO::getCreatedAt);

        Page<SensitiveTaggingRecordPO> result = repository.selectPage(page, query);
        return convertor.toRecordDomainList(result.getRecords());
    }

    @Override
    public long countRecords(String keyword, Long categoryId, Long gradeId, Boolean isLocked, String datasourceId) {
        LambdaQueryWrapper<SensitiveTaggingRecordPO> query = buildQuery(keyword, categoryId, gradeId, isLocked, datasourceId);
        Long count = repository.selectCount(query);
        return count == null ? 0 : count;
    }

    @Override
    public List<SensitiveTaggingRecord> pageRecognitionResults(int pageIndex, int pageSize, String keyword, Long categoryId, List<Long> categoryIds, Long gradeId, Boolean isLocked, String datasourceId, String maskingStatus, String recognitionMethod, String assetSourceType, Boolean hasBetterRecommendation) {
        Page<SensitiveTaggingRecordPO> page = new Page<>(pageIndex, pageSize);
        LambdaQueryWrapper<SensitiveTaggingRecordPO> query = buildRecognitionResultQuery(keyword, categoryId, categoryIds, gradeId, isLocked, datasourceId, maskingStatus, recognitionMethod, assetSourceType, hasBetterRecommendation);
        query.orderByDesc(SensitiveTaggingRecordPO::getUpdatedAt).orderByDesc(SensitiveTaggingRecordPO::getSensitivityScore);

        Page<SensitiveTaggingRecordPO> result = repository.selectPage(page, query);
        return convertor.toRecordDomainList(result.getRecords());
    }

    @Override
    public long countRecognitionResults(String keyword, Long categoryId, List<Long> categoryIds, Long gradeId, Boolean isLocked, String datasourceId, String maskingStatus, String recognitionMethod, String assetSourceType, Boolean hasBetterRecommendation) {
        LambdaQueryWrapper<SensitiveTaggingRecordPO> query = buildRecognitionResultQuery(keyword, categoryId, categoryIds, gradeId, isLocked, datasourceId, maskingStatus, recognitionMethod, assetSourceType, hasBetterRecommendation);
        Long count = repository.selectCount(query);
        return count == null ? 0 : count;
    }

    @Override
    public List<SensitiveTaggingRecord> listByCategoryId(Long categoryId) {
        if (categoryId == null) return Collections.emptyList();
        List<SensitiveTaggingRecordPO> list = repository.selectList(
            new LambdaQueryWrapper<SensitiveTaggingRecordPO>()
                .eq(SensitiveTaggingRecordPO::getCategoryId, categoryId)
                .orderByDesc(SensitiveTaggingRecordPO::getConfidenceScore)
                .orderByDesc(SensitiveTaggingRecordPO::getCreatedAt)
        );
        return convertor.toRecordDomainList(list);
    }

    @Override
    public Optional<SensitiveTaggingRecord> findById(Long id) {
        SensitiveTaggingRecordPO po = repository.selectById(id);
        return Optional.ofNullable(convertor.toDomain(po));
    }

    @Override
    public Optional<SensitiveTaggingRecord> findByTableAndField(String datasourceId, String tableName, String fieldName) {
        SensitiveTaggingRecordPO po = repository.selectOne(
            new LambdaQueryWrapper<SensitiveTaggingRecordPO>()
                .eq(SensitiveTaggingRecordPO::getDatasourceId, datasourceId)
                .eq(SensitiveTaggingRecordPO::getTableName, tableName)
                .eq(SensitiveTaggingRecordPO::getFieldName, fieldName)
        );
        return Optional.ofNullable(convertor.toDomain(po));
    }

    @Override
    public SensitiveTaggingRecord save(SensitiveTaggingRecord record) {
        SensitiveTaggingRecordPO po = convertor.toPO(record);
        if (po.getCreatedAt() == null) {
            po.setCreatedAt(LocalDateTime.now());
        }
        if (po.getUpdatedAt() == null) {
            po.setUpdatedAt(LocalDateTime.now());
        }
        repository.insert(po);
        return convertor.toDomain(po);
    }

    @Override
    public void update(SensitiveTaggingRecord record) {
        SensitiveTaggingRecordPO po = convertor.toPO(record);
        po.setUpdatedAt(LocalDateTime.now());
        repository.updateById(po);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public void deleteBatchIds(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            repository.deleteBatchIds(ids);
        }
    }

    @Override
    public void saveBatch(List<SensitiveTaggingRecord> records) {
        if (records == null || records.isEmpty()) return;
        List<SensitiveTaggingRecordPO> pos = convertor.toRecordPOList(records);
        for (SensitiveTaggingRecordPO po : pos) {
            if (po.getCreatedAt() == null) {
                po.setCreatedAt(LocalDateTime.now());
            }
            if (po.getUpdatedAt() == null) {
                po.setUpdatedAt(LocalDateTime.now());
            }
            repository.insert(po);
        }
    }

    @Override
    public void updateBatch(List<SensitiveTaggingRecord> records) {
        if (records == null || records.isEmpty()) return;
        List<SensitiveTaggingRecordPO> pos = convertor.toRecordPOList(records);
        for (SensitiveTaggingRecordPO po : pos) {
            po.setUpdatedAt(LocalDateTime.now());
            repository.updateById(po);
        }
    }

    private LambdaQueryWrapper<SensitiveTaggingRecordPO> buildQuery(String keyword, Long categoryId, Long gradeId, Boolean isLocked, String datasourceId) {
        LambdaQueryWrapper<SensitiveTaggingRecordPO> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            query.and(w -> w.like(SensitiveTaggingRecordPO::getTableName, keyword)
                .or().like(SensitiveTaggingRecordPO::getFieldName, keyword)
                .or().like(SensitiveTaggingRecordPO::getFieldComment, keyword));
        }
        if (categoryId != null) {
            query.eq(SensitiveTaggingRecordPO::getCategoryId, categoryId);
        }
        if (gradeId != null) {
            query.eq(SensitiveTaggingRecordPO::getSecurityGradeId, gradeId);
        }
        if (isLocked != null) {
            query.eq(SensitiveTaggingRecordPO::getIsLocked, isLocked);
        }
        if (datasourceId != null && !datasourceId.trim().isEmpty()) {
            query.eq(SensitiveTaggingRecordPO::getDatasourceId, datasourceId);
        }
        return query;
    }

    private LambdaQueryWrapper<SensitiveTaggingRecordPO> buildRecognitionResultQuery(String keyword, Long categoryId, List<Long> categoryIds, Long gradeId, Boolean isLocked, String datasourceId, String maskingStatus, String recognitionMethod, String assetSourceType, Boolean hasBetterRecommendation) {
        LambdaQueryWrapper<SensitiveTaggingRecordPO> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            query.and(w -> w.like(SensitiveTaggingRecordPO::getTableName, keyword)
                .or().like(SensitiveTaggingRecordPO::getFieldName, keyword)
                .or().like(SensitiveTaggingRecordPO::getFieldComment, keyword)
                .or().like(SensitiveTaggingRecordPO::getCategoryName, keyword)
                .or().like(SensitiveTaggingRecordPO::getAssetSourceInfo, keyword));
        }
        if (categoryId != null) {
            query.eq(SensitiveTaggingRecordPO::getCategoryId, categoryId);
        } else if (categoryIds != null && !categoryIds.isEmpty()) {
            query.in(SensitiveTaggingRecordPO::getCategoryId, categoryIds);
        }
        if (gradeId != null) {
            query.eq(SensitiveTaggingRecordPO::getSecurityGradeId, gradeId);
        }
        if (isLocked != null) {
            query.eq(SensitiveTaggingRecordPO::getIsLocked, isLocked);
        }
        if (datasourceId != null && !datasourceId.trim().isEmpty()) {
            query.eq(SensitiveTaggingRecordPO::getDatasourceId, datasourceId);
        }
        if (maskingStatus != null && !maskingStatus.trim().isEmpty()) {
            query.eq(SensitiveTaggingRecordPO::getMaskingStatus, maskingStatus);
        }
        if (recognitionMethod != null && !recognitionMethod.trim().isEmpty()) {
            query.eq(SensitiveTaggingRecordPO::getRecognitionMethod, recognitionMethod);
        }
        if (assetSourceType != null && !assetSourceType.trim().isEmpty()) {
            query.eq(SensitiveTaggingRecordPO::getAssetSourceType, assetSourceType);
        }
        if (hasBetterRecommendation != null) {
            query.eq(SensitiveTaggingRecordPO::getHasBetterRecommendation, hasBetterRecommendation);
        }
        return query;
    }
}
