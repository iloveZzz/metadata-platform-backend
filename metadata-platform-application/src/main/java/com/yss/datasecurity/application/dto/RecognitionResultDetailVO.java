package com.yss.datasecurity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionResultDetailVO implements Serializable {
    private static final long serialVersionUID = 1L;

    // 基本信息
    private Long id;
    private String tableName;
    private String tableComment;
    private String fieldName;
    private String fieldComment;
    private String assetSourceType;
    private String assetSourceInfo;
    private String sampleData;
    private String samplePreview;
    private Boolean sampleEnabled;

    // 当前生效结果
    private Long categoryId;
    private String categoryName;
    private Long securityGradeId;
    private String securityGradeName;
    private String recognitionMethod;
    private Integer priority;
    private Double confidenceScore;
    private String maskingStatus;
    private LocalDateTime maskingStatusUpdatedAt;
    private Boolean isLocked;
    private LocalDateTime categoryModifiedAt;
    private LocalDateTime updatedAt;

    // 是否有推荐
    private Boolean hasBetterRecommendation;
    private Long recommendedCategoryId;
    private String recommendedCategoryName;

    // 历史/多来源识别记录池
    private List<RecognitionRecordItemVO> candidateRecords;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecognitionRecordItemVO implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long recordId;
        private Long categoryId;
        private String categoryName;
        private Long securityGradeId;
        private String securityGradeName;
        private String recognitionMethod;
        private Integer priority;
        private Double confidenceScore;
        private Boolean isRecommended;
        private Boolean isCurrentEffective;
        private LocalDateTime categoryModifiedAt;
        private LocalDateTime updatedAt;
    }
}
