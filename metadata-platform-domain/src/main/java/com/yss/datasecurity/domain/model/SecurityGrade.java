package com.yss.datasecurity.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityGrade {
    private Long id;
    private String gradeName;
    private String gradeCode;
    private Integer sensitivityScore; // 1~100 整数
    private String colorTag;
    private String description;
    private Integer boundCategoriesCount;
    private Integer referencedRulesCount;
    private Integer activeFieldsCount;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    public void validateSensitivityScore() {
        if (sensitivityScore == null || sensitivityScore < 1 || sensitivityScore > 100) {
            throw new IllegalArgumentException("敏感程度权重必须为 1~100 之间的整数！");
        }
    }
}
