package com.yss.datasecurity.domain.model;

import com.yss.datasecurity.domain.constant.RecognitionRuleConstants;
import com.yss.datasecurity.domain.exception.DataSecurityErrorCode;
import com.yss.datasecurity.domain.exception.DataSecurityException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveRule {
    private Long id;
    private String ruleName;
    private String ruleType; // BUILTIN / CUSTOM
    private String description;
    private Integer priority; // 1~100, 1最高
    private String owner;
    private String status; // ENABLED / DISABLED
    private String categoryScopeMode; // ALL / TREE_NODE / SPECIFIC
    private List<Long> categoryScopeIds;
    private String scanScopeType; // COMPUTE_ENGINE / DATASOURCE
    private String scanScopeConfig; // JSON 存储
    private String featureConfig; // JSON 存储多模态特征
    private Integer taggedFieldsCount;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    public void validatePriority() {
        if (priority == null || priority < RecognitionRuleConstants.MIN_PRIORITY || priority > RecognitionRuleConstants.MAX_PRIORITY) {
            throw new DataSecurityException(DataSecurityErrorCode.INVALID_RULE_PRIORITY,
                    "规则优先级必须在 " + RecognitionRuleConstants.MIN_PRIORITY + "~" + RecognitionRuleConstants.MAX_PRIORITY + " 范围内");
        }
    }
}
