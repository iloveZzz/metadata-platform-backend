package com.yss.datamiddle.smartgovernance.infrastructure.repository.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sg_classification_rule")
public class ClassificationRulePO {
    @TableId
    private String id;
    private String templateId;
    private String sensitiveType;
    private String sensitiveName;
    private String securityLevel;
    private String clauseRef;
    private String regexPattern;
    private String dictionaryWords;
    private String semanticPrompt;
    private Integer isActive;
    private Integer priority;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
