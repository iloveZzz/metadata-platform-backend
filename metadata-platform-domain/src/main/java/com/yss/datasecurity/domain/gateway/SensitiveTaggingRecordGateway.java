package com.yss.datasecurity.domain.gateway;

import com.yss.datasecurity.domain.model.SensitiveTaggingRecord;

import java.util.List;
import java.util.Optional;

public interface SensitiveTaggingRecordGateway {
    List<SensitiveTaggingRecord> pageRecords(int pageIndex, int pageSize, String keyword, Long categoryId, Long gradeId, Boolean isLocked, String datasourceId);
    long countRecords(String keyword, Long categoryId, Long gradeId, Boolean isLocked, String datasourceId);
    
    List<SensitiveTaggingRecord> pageRecognitionResults(int pageIndex, int pageSize, String keyword, Long categoryId, List<Long> categoryIds, Long gradeId, Boolean isLocked, String datasourceId, String maskingStatus, String recognitionMethod, String assetSourceType, Boolean hasBetterRecommendation);
    long countRecognitionResults(String keyword, Long categoryId, List<Long> categoryIds, Long gradeId, Boolean isLocked, String datasourceId, String maskingStatus, String recognitionMethod, String assetSourceType, Boolean hasBetterRecommendation);

    List<SensitiveTaggingRecord> listByCategoryId(Long categoryId);
    Optional<SensitiveTaggingRecord> findById(Long id);
    Optional<SensitiveTaggingRecord> findByTableAndField(String datasourceId, String tableName, String fieldName);
    SensitiveTaggingRecord save(SensitiveTaggingRecord record);
    void update(SensitiveTaggingRecord record);
    void deleteById(Long id);
    void deleteBatchIds(List<Long> ids);
    void saveBatch(List<SensitiveTaggingRecord> records);
    void updateBatch(List<SensitiveTaggingRecord> records);
}
