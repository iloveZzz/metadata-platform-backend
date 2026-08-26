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
public class SensitiveRuleVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String ruleName;
    private String ruleType; // BUILTIN / CUSTOM
    private String description;
    private Integer priority;
    private String owner;
    private String status; // ENABLED / DISABLED
    private String categoryScopeMode; // ALL / TREE_NODE / SPECIFIC
    private String scanScopeType; // COMPUTE_ENGINE / DATASOURCE
    private Object featureConfig;
    private Integer taggedFieldsCount;
    private LocalDateTime updatedAt;
}
