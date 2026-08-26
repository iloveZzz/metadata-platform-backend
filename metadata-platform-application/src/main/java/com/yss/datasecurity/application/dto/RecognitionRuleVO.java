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
public class RecognitionRuleVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String ruleName;
    private String description;
    private String categoryScopeMode;
    private Object categoryScopeConfig;
    private String scanSourceType;
    private Object computeScopeConfig;
    private Object datasourceScopeConfig;
    private String owner;
    private String status; // ENABLED / DISABLED
    private Integer priority;
    private Integer taggedFieldsCount;
    private Boolean lineageInheritanceEnabled;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
