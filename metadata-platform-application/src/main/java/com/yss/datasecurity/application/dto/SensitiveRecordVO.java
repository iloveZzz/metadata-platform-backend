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
public class SensitiveRecordVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String datasourceId;
    private String datasourceName;
    private String tableName;
    private String fieldName;
    private String fieldComment;
    private Long categoryId;
    private String categoryName;
    private Long securityGradeId;
    private String securityGradeName;
    private Integer sensitivityScore;
    private String matchedRuleName;
    private String sourceType; // RULE_AUTO / MANUAL_LOCKED
    private Boolean isLocked;
    private LocalDateTime updatedAt;
}
