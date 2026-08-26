package com.yss.datasecurity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String tableName;
    private String tableComment;
    private String fieldName;
    private String fieldComment;
    private String assetSourceType; // DATAPHIN / DATASOURCE
    private String assetSourceInfo; // e.g. "fashion_cdm_dev (服饰CDM项目) / LD_Fashion_dev (服饰零售_开发)"
    private String datasourceId;
    private String datasourceName;
    private String schemaName;

    private Long categoryId;
    private String categoryName;
    private Long securityGradeId;
    private String securityGradeName;

    private String maskingStatus; // ENABLED / DISABLED
    private LocalDateTime maskingStatusUpdatedAt;

    private String recognitionMethod; // AUTO / MANUAL / LINEAGE
    private Boolean isLocked;
    private String lockUser;
    private LocalDateTime lockTime;

    private Integer priority;
    private Double confidenceScore;
    private String sampleData;
    private String samplePreview;

    private Boolean hasBetterRecommendation;
    private Long recommendedCategoryId;
    private String recommendedCategoryName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
