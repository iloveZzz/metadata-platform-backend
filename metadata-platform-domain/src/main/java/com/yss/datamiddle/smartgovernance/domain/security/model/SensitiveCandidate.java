package com.yss.datamiddle.smartgovernance.domain.security.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 安全打标识别候选 (聚合根)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveCandidate implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String templateId;
    private String ruleId;
    private String dataSource;
    private String databaseName;
    private String tableName;
    private String columnName;
    private String columnComment;
    private String dataType;
    private String sensitiveType;
    private SecurityLevel recommendedLevel;
    private String clauseRef;
    private String reasoning;
    private BigDecimal confidence;
    private FunnelLayer funnelLayer;
    private CandidateStatus status;
    private SecurityLevel actualLevel;
    private String operator;
    private String reviewComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void approve(String operator) {
        this.status = CandidateStatus.APPROVED;
        this.actualLevel = this.recommendedLevel;
        this.operator = operator;
        this.updatedAt = LocalDateTime.now();
    }

    public void modify(SecurityLevel targetLevel, String targetSensitiveType, String operator, String reason) {
        this.status = CandidateStatus.MODIFIED;
        this.actualLevel = targetLevel;
        if (targetSensitiveType != null && !targetSensitiveType.isEmpty()) {
            this.sensitiveType = targetSensitiveType;
        }
        this.operator = operator;
        this.reviewComment = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void ignore(String operator, String reason) {
        this.status = CandidateStatus.IGNORED;
        this.operator = operator;
        this.reviewComment = reason;
        this.updatedAt = LocalDateTime.now();
    }
}
