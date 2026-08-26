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
public class SecurityGradeVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String gradeName;
    private String gradeCode;
    private Integer sensitivityScore;
    private String colorTag;
    private String description;
    private Integer boundCategoriesCount;
    private Integer referencedRulesCount;
    private Integer activeFieldsCount;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
