package com.yss.datasecurity.domain.model;

import com.yss.datasecurity.domain.enums.CommonStatusEnum;
import com.yss.datasecurity.domain.enums.RecognitionMethodEnum;
import com.yss.datasecurity.domain.enums.RecognitionSourceTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveTaggingRecord {
    private Long id;
    private String datasourceId;
    private String datasourceName;
    private String schemaName;
    private String tableName;
    private String fieldName;
    private String fieldComment;
    private Long categoryId;
    private String categoryName;
    private Long securityGradeId;
    private String securityGradeName;
    private Integer sensitivityScore;
    private Long matchedRuleId;
    private String matchedRuleName;
    private String sourceType; // RULE_AUTO / MANUAL_LOCKED
    private Boolean isLocked;
    private String lockUser;
    private LocalDateTime lockTime;
    private String sampleData;
    private String samplePreview;
    private Double confidenceScore;
    private String status; // ACTIVE / EXPIRED / CONFIRMED / UNCONFIRMED
    private String maskingStatus; // ENABLED / DISABLED
    private LocalDateTime maskingStatusUpdatedAt;
    private String recognitionMethod; // AUTO / MANUAL / LINEAGE
    private String assetSourceType; // DATAPHIN / DATASOURCE
    private String assetSourceInfo;
    private Long recommendedCategoryId;
    private String recommendedCategoryName;
    private Boolean hasBetterRecommendation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void calibrate(Long newCategoryId, String newCategoryName, Long newGradeId, String newGradeName, Integer newScore, boolean permanentLock, String operator) {
        this.categoryId = newCategoryId;
        this.categoryName = newCategoryName;
        this.securityGradeId = newGradeId;
        this.securityGradeName = newGradeName;
        this.sensitivityScore = newScore;
        this.sourceType = RecognitionSourceTypeEnum.MANUAL_LOCKED.getCode();
        this.recognitionMethod = RecognitionMethodEnum.MANUAL.getCode();
        this.isLocked = permanentLock;
        this.lockUser = operator;
        this.lockTime = LocalDateTime.now();
        this.hasBetterRecommendation = false;
        this.recommendedCategoryId = null;
        this.recommendedCategoryName = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void toggleMaskingStatus(boolean enabled) {
        this.maskingStatus = enabled ? CommonStatusEnum.ENABLED.getCode() : CommonStatusEnum.DISABLED.getCode();
        this.maskingStatusUpdatedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void lock(String operator) {
        this.isLocked = true;
        this.lockUser = operator;
        this.lockTime = LocalDateTime.now();
        this.recognitionMethod = RecognitionMethodEnum.MANUAL.getCode();
        this.updatedAt = LocalDateTime.now();
    }

    public void unlock() {
        this.isLocked = false;
        this.lockUser = null;
        this.lockTime = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void adoptRecommendation(Long targetCategoryId, String targetCategoryName, Long targetGradeId, String targetGradeName, String operator) {
        this.categoryId = targetCategoryId;
        this.categoryName = targetCategoryName;
        if (targetGradeId != null) {
            this.securityGradeId = targetGradeId;
            this.securityGradeName = targetGradeName;
        }
        this.recognitionMethod = RecognitionMethodEnum.MANUAL.getCode();
        this.hasBetterRecommendation = false;
        this.recommendedCategoryId = null;
        this.recommendedCategoryName = null;
        this.lockUser = operator;
        this.lockTime = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
