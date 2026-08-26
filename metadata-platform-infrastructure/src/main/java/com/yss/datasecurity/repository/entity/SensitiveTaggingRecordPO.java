package com.yss.datasecurity.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("sec_sensitive_record")
public class SensitiveTaggingRecordPO implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("datasource_id")
    private String datasourceId;

    @TableField("datasource_name")
    private String datasourceName;

    @TableField("schema_name")
    private String schemaName;

    @TableField("table_name")
    private String tableName;

    @TableField("field_name")
    private String fieldName;

    @TableField("field_comment")
    private String fieldComment;

    @TableField("category_id")
    private Long categoryId;

    @TableField("category_name")
    private String categoryName;

    @TableField("security_grade_id")
    private Long securityGradeId;

    @TableField("security_grade_name")
    private String securityGradeName;

    @TableField("sensitivity_score")
    private Integer sensitivityScore;

    @TableField("matched_rule_id")
    private Long matchedRuleId;

    @TableField("matched_rule_name")
    private String matchedRuleName;

    @TableField("source_type")
    private String sourceType;

    @TableField("is_locked")
    private Boolean isLocked;

    @TableField("lock_user")
    private String lockUser;

    @TableField("lock_time")
    private LocalDateTime lockTime;

    @TableField("sample_data")
    private String sampleData;

    @TableField("sample_preview")
    private String samplePreview;

    @TableField("confidence_score")
    private Double confidenceScore;

    @TableField("status")
    private String status;

    @TableField("masking_status")
    private String maskingStatus;

    @TableField("masking_status_updated_at")
    private LocalDateTime maskingStatusUpdatedAt;

    @TableField("recognition_method")
    private String recognitionMethod;

    @TableField("asset_source_type")
    private String assetSourceType;

    @TableField("asset_source_info")
    private String assetSourceInfo;

    @TableField("recommended_category_id")
    private Long recommendedCategoryId;

    @TableField("recommended_category_name")
    private String recommendedCategoryName;

    @TableField("has_better_recommendation")
    private Boolean hasBetterRecommendation;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
