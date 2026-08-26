package com.yss.datasecurity.application.service.impl;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.dto.RecognitionBatchLogVO;
import com.yss.datasecurity.application.dto.RecognitionResultDetailVO;
import com.yss.datasecurity.application.dto.RecognitionResultEditDTO;
import com.yss.datasecurity.application.dto.RecognitionResultImportPreviewVO;
import com.yss.datasecurity.application.dto.RecognitionResultManualAddDTO;
import com.yss.datasecurity.application.dto.RecognitionResultPageQueryDTO;
import com.yss.datasecurity.application.dto.RecognitionResultVO;
import com.yss.datasecurity.application.service.RecognitionResultAppService;
import com.yss.datasecurity.domain.exception.DataSecurityException;
import com.yss.datasecurity.domain.gateway.DataCategoryGateway;
import com.yss.datasecurity.domain.gateway.SecurityGradeGateway;
import com.yss.datasecurity.domain.gateway.SensitiveTaggingRecordGateway;
import com.yss.datasecurity.domain.model.DataCategory;
import com.yss.datasecurity.domain.model.SecurityGrade;
import com.yss.datasecurity.domain.model.SensitiveTaggingRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecognitionResultAppServiceImpl implements RecognitionResultAppService {

    private final SensitiveTaggingRecordGateway recordGateway;
    private final DataCategoryGateway dataCategoryGateway;
    private final SecurityGradeGateway securityGradeGateway;

    // 内存模拟导入历史
    private static final List<RecognitionBatchLogVO> IMPORT_LOGS = new ArrayList<>(Arrays.asList(
            RecognitionBatchLogVO.builder()
                    .id(8001L)
                    .batchType("IMPORT")
                    .fileName("dataphin_sensitive_tagging_v1.xlsx")
                    .assetType("DATAPHIN")
                    .totalCount(120)
                    .successCount(120)
                    .failedCount(0)
                    .conflictStrategy("OVERWRITE_ALL")
                    .maskingPolicy("UNIFIED_ENABLED")
                    .status("SUCCESS")
                    .operator("admin")
                    .createdAt(LocalDateTime.of(2025, 3, 16, 10, 0, 0))
                    .build(),
            RecognitionBatchLogVO.builder()
                    .id(8002L)
                    .batchType("MANUAL_ADD")
                    .fileName("按表批量添加 (8张表/26个字段)")
                    .assetType("DATAPHIN")
                    .totalCount(26)
                    .successCount(26)
                    .failedCount(0)
                    .conflictStrategy("OVERWRITE_UNLOCKED")
                    .maskingPolicy("RETAIN_CONFIG")
                    .status("SUCCESS")
                    .operator("admin")
                    .createdAt(LocalDateTime.of(2025, 3, 17, 11, 20, 0))
                    .build()
    ));

    @Override
    public PageResult<RecognitionResultVO> pageRecognitionResults(RecognitionResultPageQueryDTO query) {
        if (query == null) {
            query = RecognitionResultPageQueryDTO.builder().build();
        }
        int pageIndex = query.getPageIndex() > 0 ? query.getPageIndex() : 1;
        int pageSize = query.getPageSize() > 0 ? query.getPageSize() : 20;

        Long targetCategoryId = query.getCategoryId();
        List<Long> targetCategoryIds = query.getCategoryIds();

        // 当指定分类目录节点且未直接指定分类时，递归汇聚该目录节点下所有分类
        if (targetCategoryId == null && (targetCategoryIds == null || targetCategoryIds.isEmpty())
                && query.getTreeNodeId() != null && query.getTreeNodeId() > 0) {
            List<DataCategory> cats = dataCategoryGateway.listAll(query.getTreeNodeId(), null, null);
            if (cats.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0, pageSize, pageIndex);
            }
            targetCategoryIds = cats.stream().map(DataCategory::getId).collect(Collectors.toList());
        }

        List<SensitiveTaggingRecord> domainList = recordGateway.pageRecognitionResults(
                pageIndex, pageSize, query.getKeyword(), targetCategoryId, targetCategoryIds,
                query.getSecurityGradeId(), query.getIsLocked(), query.getDatasourceId(),
                query.getMaskingStatus(), query.getRecognitionMethod(), query.getAssetSourceType(),
                query.getHasBetterRecommendation()
        );
        long total = recordGateway.countRecognitionResults(
                query.getKeyword(), targetCategoryId, targetCategoryIds,
                query.getSecurityGradeId(), query.getIsLocked(), query.getDatasourceId(),
                query.getMaskingStatus(), query.getRecognitionMethod(), query.getAssetSourceType(),
                query.getHasBetterRecommendation()
        );

        List<RecognitionResultVO> voList = domainList.stream().map(this::toResultVO).collect(Collectors.toList());
        return PageResult.of(voList, (int) total, pageSize, pageIndex);
    }

    @Override
    public RecognitionResultDetailVO getRecognitionResultDetail(Long id) {
        SensitiveTaggingRecord record = recordGateway.findById(id)
                .orElseThrow(() -> new DataSecurityException("RESULT_NOT_FOUND", "识别结果记录不存在: " + id));

        // 构造候选识别记录池
        List<RecognitionResultDetailVO.RecognitionRecordItemVO> candidates = new ArrayList<>();
        
        // 1. 当前生效记录
        candidates.add(RecognitionResultDetailVO.RecognitionRecordItemVO.builder()
                .recordId(record.getId())
                .categoryId(record.getCategoryId())
                .categoryName(record.getCategoryName())
                .securityGradeId(record.getSecurityGradeId())
                .securityGradeName(record.getSecurityGradeName())
                .recognitionMethod(record.getRecognitionMethod() != null ? record.getRecognitionMethod() : "AUTO")
                .priority(record.getMatchedRuleId() != null ? 10 : 20)
                .confidenceScore(record.getConfidenceScore() != null ? record.getConfidenceScore() : 90.0)
                .isRecommended(false)
                .isCurrentEffective(true)
                .categoryModifiedAt(record.getUpdatedAt())
                .updatedAt(record.getUpdatedAt())
                .build());

        // 2. 若存在更优推荐结果，追加推荐候选记录
        if (Boolean.TRUE.equals(record.getHasBetterRecommendation()) && record.getRecommendedCategoryId() != null) {
            candidates.add(RecognitionResultDetailVO.RecognitionRecordItemVO.builder()
                    .recordId(record.getId() + 100000L)
                    .categoryId(record.getRecommendedCategoryId())
                    .categoryName(record.getRecommendedCategoryName())
                    .securityGradeId(record.getSecurityGradeId() != null ? record.getSecurityGradeId() : 3L)
                    .securityGradeName(record.getSecurityGradeName() != null ? record.getSecurityGradeName() : "L3")
                    .recognitionMethod("AUTO")
                    .priority(5)
                    .confidenceScore(99.5)
                    .isRecommended(true)
                    .isCurrentEffective(false)
                    .categoryModifiedAt(record.getUpdatedAt())
                    .updatedAt(record.getUpdatedAt())
                    .build());
        }

        return RecognitionResultDetailVO.builder()
                .id(record.getId())
                .tableName(record.getTableName())
                .tableComment(resolveTableComment(record.getTableName()))
                .fieldName(record.getFieldName())
                .fieldComment(record.getFieldComment())
                .assetSourceType(record.getAssetSourceType() != null ? record.getAssetSourceType() : "DATAPHIN")
                .assetSourceInfo(record.getAssetSourceInfo() != null ? record.getAssetSourceInfo() : record.getDatasourceName())
                .sampleData(record.getSampleData())
                .samplePreview(record.getSamplePreview() != null ? record.getSamplePreview() : record.getSampleData())
                .sampleEnabled(true)
                .categoryId(record.getCategoryId())
                .categoryName(record.getCategoryName())
                .securityGradeId(record.getSecurityGradeId())
                .securityGradeName(record.getSecurityGradeName())
                .recognitionMethod(record.getRecognitionMethod() != null ? record.getRecognitionMethod() : "AUTO")
                .priority(record.getMatchedRuleId() != null ? 10 : 20)
                .confidenceScore(record.getConfidenceScore() != null ? record.getConfidenceScore() : 90.0)
                .maskingStatus(record.getMaskingStatus() != null ? record.getMaskingStatus() : "ENABLED")
                .maskingStatusUpdatedAt(record.getMaskingStatusUpdatedAt() != null ? record.getMaskingStatusUpdatedAt() : record.getUpdatedAt())
                .isLocked(Boolean.TRUE.equals(record.getIsLocked()))
                .categoryModifiedAt(record.getUpdatedAt())
                .updatedAt(record.getUpdatedAt())
                .hasBetterRecommendation(record.getHasBetterRecommendation())
                .recommendedCategoryId(record.getRecommendedCategoryId())
                .recommendedCategoryName(record.getRecommendedCategoryName())
                .candidateRecords(candidates)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMaskingStatus(Long id, String status) {
        SensitiveTaggingRecord record = recordGateway.findById(id)
                .orElseThrow(() -> new DataSecurityException("RESULT_NOT_FOUND", "识别结果记录不存在: " + id));
        record.toggleMaskingStatus("ENABLED".equalsIgnoreCase(status));
        recordGateway.update(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateMaskingStatus(List<Long> ids, String status) {
        if (ids == null || ids.isEmpty()) return;
        boolean enabled = "ENABLED".equalsIgnoreCase(status);
        for (Long id : ids) {
            recordGateway.findById(id).ifPresent(record -> {
                record.toggleMaskingStatus(enabled);
                recordGateway.update(record);
            });
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockResult(Long id, boolean isLocked) {
        SensitiveTaggingRecord record = recordGateway.findById(id)
                .orElseThrow(() -> new DataSecurityException("RESULT_NOT_FOUND", "识别结果记录不存在: " + id));
        if (isLocked) {
            record.lock("admin");
        } else {
            record.unlock();
        }
        recordGateway.update(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchLockResults(List<Long> ids, boolean isLocked) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            recordGateway.findById(id).ifPresent(record -> {
                if (isLocked) {
                    record.lock("admin");
                } else {
                    record.unlock();
                }
                recordGateway.update(record);
            });
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editResult(RecognitionResultEditDTO dto) {
        if (dto.getIds() == null || dto.getIds().isEmpty()) return;
        DataCategory category = null;
        SecurityGrade grade = null;
        if (dto.getCategoryId() != null) {
            category = dataCategoryGateway.findById(dto.getCategoryId())
                    .orElseThrow(() -> new DataSecurityException("CATEGORY_NOT_FOUND", "目标数据分类不存在: " + dto.getCategoryId()));
            if (category.getSecurityGradeId() != null) {
                grade = securityGradeGateway.findById(category.getSecurityGradeId()).orElse(null);
            }
        }

        for (Long id : dto.getIds()) {
            final DataCategory finalCategory = category;
            final SecurityGrade finalGrade = grade;
            recordGateway.findById(id).ifPresent(record -> {
                if (finalCategory != null) {
                    record.calibrate(
                            finalCategory.getId(),
                            finalCategory.getCategoryName(),
                            finalGrade != null ? finalGrade.getId() : null,
                            finalGrade != null ? finalGrade.getGradeName() : null,
                            finalGrade != null ? finalGrade.getSensitivityScore() : 50,
                            "MANUAL".equalsIgnoreCase(dto.getRecognitionMethod()),
                            "admin"
                    );
                } else if (dto.getRecognitionMethod() != null) {
                    if ("MANUAL".equalsIgnoreCase(dto.getRecognitionMethod())) {
                        record.lock("admin");
                    } else {
                        record.unlock();
                    }
                }
                if (Boolean.TRUE.equals(dto.getSyncMaskingStatus())) {
                    record.toggleMaskingStatus(true);
                }
                recordGateway.update(record);
            });
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchEditResults(RecognitionResultEditDTO dto) {
        editResult(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adoptRecommendation(Long id, Long candidateCategoryId) {
        SensitiveTaggingRecord record = recordGateway.findById(id)
                .orElseThrow(() -> new DataSecurityException("RESULT_NOT_FOUND", "识别结果记录不存在: " + id));
        Long targetCatId = candidateCategoryId != null ? candidateCategoryId : record.getRecommendedCategoryId();
        if (targetCatId == null) {
            throw new DataSecurityException("NO_RECOMMENDATION", "当前记录无推荐打标结果");
        }

        DataCategory category = dataCategoryGateway.findById(targetCatId)
                .orElseThrow(() -> new DataSecurityException("CATEGORY_NOT_FOUND", "推荐数据分类不存在: " + targetCatId));

        SecurityGrade grade = null;
        if (category.getSecurityGradeId() != null) {
            grade = securityGradeGateway.findById(category.getSecurityGradeId()).orElse(null);
        }

        record.adoptRecommendation(
                category.getId(),
                category.getCategoryName(),
                grade != null ? grade.getId() : null,
                grade != null ? grade.getGradeName() : null,
                "admin"
        );
        recordGateway.update(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteResult(Long id) {
        recordGateway.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteResults(List<Long> ids) {
        recordGateway.deleteBatchIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualAdd(RecognitionResultManualAddDTO dto) {
        String dedupStrategy = dto.getDedupStrategy() != null ? dto.getDedupStrategy() : "OVERWRITE_ALL";

        int successCount = 0;
        for (RecognitionResultManualAddDTO.ManualAddRecordItemDTO item : dto.getRecords()) {
            String dsId = item.getDatasourceId() != null ? item.getDatasourceId() : "default_ds";
            Optional<SensitiveTaggingRecord> existingOpt = recordGateway.findByTableAndField(dsId, item.getTableName(), item.getFieldName());

            DataCategory category = dataCategoryGateway.findById(item.getCategoryId())
                    .orElseThrow(() -> new DataSecurityException("CATEGORY_NOT_FOUND", "数据分类不存在: " + item.getCategoryId()));
            SecurityGrade grade = null;
            if (category.getSecurityGradeId() != null) {
                grade = securityGradeGateway.findById(category.getSecurityGradeId()).orElse(null);
            }

            if (existingOpt.isPresent()) {
                SensitiveTaggingRecord existing = existingOpt.get();
                if ("RETAIN_EXISTING".equalsIgnoreCase(dedupStrategy)) {
                    // 保留已有，跳过
                    continue;
                }
                if ("OVERWRITE_UNLOCKED".equalsIgnoreCase(dedupStrategy) && Boolean.TRUE.equals(existing.getIsLocked())) {
                    // 仅覆盖未锁定，已锁定跳过
                    continue;
                }
                // 执行覆盖
                existing.calibrate(
                        category.getId(),
                        category.getCategoryName(),
                        grade != null ? grade.getId() : 1L,
                        grade != null ? grade.getGradeName() : "L1",
                        grade != null ? grade.getSensitivityScore() : 1,
                        true,
                        "admin"
                );
                if (item.getMaskingStatus() != null) {
                    existing.toggleMaskingStatus("ENABLED".equalsIgnoreCase(item.getMaskingStatus()));
                }
                recordGateway.update(existing);
                successCount++;
            } else {
                // 新建打标
                SensitiveTaggingRecord newRecord = SensitiveTaggingRecord.builder()
                        .id(System.currentTimeMillis() + (long)(Math.random() * 10000))
                        .datasourceId(dsId)
                        .datasourceName(item.getDatasourceName() != null ? item.getDatasourceName() : dsId)
                        .schemaName(item.getSchemaName() != null ? item.getSchemaName() : "default_schema")
                        .tableName(item.getTableName())
                        .fieldName(item.getFieldName())
                        .fieldComment(item.getFieldComment())
                        .categoryId(category.getId())
                        .categoryName(category.getCategoryName())
                        .securityGradeId(grade != null ? grade.getId() : 1L)
                        .securityGradeName(grade != null ? grade.getGradeName() : "L1")
                        .sensitivityScore(grade != null ? grade.getSensitivityScore() : 1)
                        .sourceType("MANUAL_LOCKED")
                        .recognitionMethod("MANUAL")
                        .isLocked(true)
                        .lockUser("admin")
                        .lockTime(LocalDateTime.now())
                        .maskingStatus(item.getMaskingStatus() != null ? item.getMaskingStatus() : "ENABLED")
                        .maskingStatusUpdatedAt(LocalDateTime.now())
                        .assetSourceType(item.getAssetSourceType() != null ? item.getAssetSourceType() : "DATAPHIN")
                        .assetSourceInfo(item.getAssetSourceInfo() != null ? item.getAssetSourceInfo() : (item.getDatasourceName() != null ? item.getDatasourceName() : item.getTableName()))
                        .hasBetterRecommendation(false)
                        .status("CONFIRMED")
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                recordGateway.save(newRecord);
                successCount++;
            }
        }

        // 记录批量操作流水
        IMPORT_LOGS.add(0, RecognitionBatchLogVO.builder()
                .id(System.currentTimeMillis())
                .batchType("MANUAL_ADD")
                .fileName("按表手动添加 (" + dto.getRecords().size() + "个字段)")
                .assetType("DATAPHIN")
                .totalCount(dto.getRecords().size())
                .successCount(successCount)
                .failedCount(dto.getRecords().size() - successCount)
                .conflictStrategy(dedupStrategy)
                .maskingPolicy("AUTO_APPLY")
                .status(successCount == dto.getRecords().size() ? "SUCCESS" : "PARTIAL_FAILED")
                .operator("admin")
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    public RecognitionResultImportPreviewVO importPreview(String assetType, String conflictStrategy, String maskingPolicy) {
        List<RecognitionResultImportPreviewVO.ImportRowPreviewItemVO> validRows = Arrays.asList(
                RecognitionResultImportPreviewVO.ImportRowPreviewItemVO.builder()
                        .rowNumber(2)
                        .tableName("fct_trade_settlement_di")
                        .fieldName("settle_bank_acc")
                        .categoryName("银行卡号 (/财务信息/)")
                        .securityGradeName("L3")
                        .status("VALID")
                        .build(),
                RecognitionResultImportPreviewVO.ImportRowPreviewItemVO.builder()
                        .rowNumber(3)
                        .tableName("dim_user_passport")
                        .fieldName("id_card_num")
                        .categoryName("居民身份证(中国大陆)")
                        .securityGradeName("L4")
                        .status("VALID")
                        .build()
        );

        List<RecognitionResultImportPreviewVO.ImportRowPreviewItemVO> duplicateRows = Collections.singletonList(
                RecognitionResultImportPreviewVO.ImportRowPreviewItemVO.builder()
                        .rowNumber(4)
                        .tableName("fct_pay_order_di")
                        .fieldName("pay_order_no")
                        .categoryName("订单信息 (/交易信息/)")
                        .securityGradeName("L2")
                        .onlineCategoryName("订单信息 (/交易信息/)")
                        .status("DUPLICATE")
                        .errorMessage("与线上已有识别记录重复 (命中去重策略)")
                        .build()
        );

        return RecognitionResultImportPreviewVO.builder()
                .totalCount(3)
                .validCount(2)
                .duplicateCount(1)
                .errorCount(0)
                .validRows(validRows)
                .duplicateRows(duplicateRows)
                .errorRows(Collections.emptyList())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importExecute(String assetType, String conflictStrategy, String maskingPolicy, String fileName) {
        // 执行导入样本落库
        SensitiveTaggingRecord r1 = SensitiveTaggingRecord.builder()
                .id(System.currentTimeMillis() + 10)
                .datasourceId("dataphin_fashion_cdm")
                .datasourceName("fashion_cdm_dev")
                .schemaName("LD_Fashion_dev")
                .tableName("fct_trade_settlement_di")
                .fieldName("settle_bank_acc")
                .fieldComment("结算银行账号")
                .categoryId(1014L)
                .categoryName("银行卡号 (/财务信息/)")
                .securityGradeId(3L)
                .securityGradeName("L3")
                .sensitivityScore(3)
                .sourceType("MANUAL_LOCKED")
                .recognitionMethod("MANUAL")
                .isLocked(true)
                .lockUser("admin")
                .lockTime(LocalDateTime.now())
                .maskingStatus("ENABLED")
                .maskingStatusUpdatedAt(LocalDateTime.now())
                .assetSourceType(assetType != null ? assetType : "DATAPHIN")
                .assetSourceInfo("fashion_cdm_dev (服饰结算项目) / LD_Fashion_dev (服饰结算)")
                .hasBetterRecommendation(false)
                .status("CONFIRMED")
                .sampleData("622202100018273619")
                .samplePreview("622202******3619")
                .confidenceScore(99.0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        recordGateway.save(r1);

        IMPORT_LOGS.add(0, RecognitionBatchLogVO.builder()
                .id(System.currentTimeMillis())
                .batchType("IMPORT")
                .fileName(fileName != null ? fileName : "batch_import_data.xlsx")
                .assetType(assetType != null ? assetType : "DATAPHIN")
                .totalCount(2)
                .successCount(2)
                .failedCount(0)
                .conflictStrategy(conflictStrategy)
                .maskingPolicy(maskingPolicy)
                .status("SUCCESS")
                .operator("admin")
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    public List<RecognitionBatchLogVO> listImportHistory() {
        return new ArrayList<>(IMPORT_LOGS);
    }

    private RecognitionResultVO toResultVO(SensitiveTaggingRecord r) {
        return RecognitionResultVO.builder()
                .id(r.getId())
                .tableName(r.getTableName())
                .tableComment(resolveTableComment(r.getTableName()))
                .fieldName(r.getFieldName())
                .fieldComment(r.getFieldComment())
                .assetSourceType(r.getAssetSourceType() != null ? r.getAssetSourceType() : "DATAPHIN")
                .assetSourceInfo(r.getAssetSourceInfo() != null ? r.getAssetSourceInfo() : r.getDatasourceName())
                .datasourceId(r.getDatasourceId())
                .datasourceName(r.getDatasourceName())
                .schemaName(r.getSchemaName())
                .categoryId(r.getCategoryId())
                .categoryName(r.getCategoryName())
                .securityGradeId(r.getSecurityGradeId())
                .securityGradeName(r.getSecurityGradeName())
                .maskingStatus(r.getMaskingStatus() != null ? r.getMaskingStatus() : "ENABLED")
                .maskingStatusUpdatedAt(r.getMaskingStatusUpdatedAt() != null ? r.getMaskingStatusUpdatedAt() : r.getUpdatedAt())
                .recognitionMethod(r.getRecognitionMethod() != null ? r.getRecognitionMethod() : "AUTO")
                .isLocked(Boolean.TRUE.equals(r.getIsLocked()))
                .lockUser(r.getLockUser())
                .lockTime(r.getLockTime())
                .priority(r.getMatchedRuleId() != null ? 10 : 20)
                .confidenceScore(r.getConfidenceScore() != null ? r.getConfidenceScore() : 88.0)
                .sampleData(r.getSampleData())
                .samplePreview(r.getSamplePreview() != null ? r.getSamplePreview() : r.getSampleData())
                .hasBetterRecommendation(r.getHasBetterRecommendation())
                .recommendedCategoryId(r.getRecommendedCategoryId())
                .recommendedCategoryName(r.getRecommendedCategoryName())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private String resolveTableComment(String tableName) {
        if (tableName == null) return "-";
        if (tableName.contains("user") || tableName.contains("usr")) return "用户核心信息表";
        if (tableName.contains("order") || tableName.contains("pay")) return "订单支付明细表";
        if (tableName.contains("cust") || tableName.contains("account")) return "客户账户维度表";
        if (tableName.contains("trade") || tableName.contains("settle")) return "交易清结算日志表";
        if (tableName.contains("sec_") || tableName.contains("tag")) return "数据安全打标元数据表";
        return "业务数据表 (" + tableName + ")";
    }
}
