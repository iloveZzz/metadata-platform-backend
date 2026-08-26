package com.yss.datamiddle.smartgovernance.domain.security.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分类分级识别规则
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationRule implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String templateId;
    private String sensitiveType;
    private String sensitiveName;
    private SecurityLevel securityLevel;
    private String clauseRef;
    private String regexPattern;
    private String dictionaryWords;
    private String semanticPrompt;
    private Boolean isActive;
    private Integer priority;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
