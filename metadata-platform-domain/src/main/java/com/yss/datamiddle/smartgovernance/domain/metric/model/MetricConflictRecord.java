package com.yss.datamiddle.smartgovernance.domain.metric.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 指标语义与 AST 冲突事件 (聚合根)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricConflictRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String conflictCode;
    private String indicatorAId;
    private String indicatorAName;
    private String indicatorACode;
    private String indicatorADomain;
    private String indicatorBId;
    private String indicatorBName;
    private String indicatorBCode;
    private String indicatorBDomain;
    private ConflictType conflictType;
    private BigDecimal similarityScore;
    private String formulaA;
    private String formulaB;
    private String astDiffSummary;
    private ConflictStatus status;
    private String canonicalId;
    private String resolutionComment;
    private String operator;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void reconcile(String canonicalId, String operator, String comment) {
        this.status = ConflictStatus.RESOLVED;
        this.canonicalId = canonicalId;
        this.operator = operator;
        this.resolutionComment = comment;
        this.updatedAt = LocalDateTime.now();
    }

    public void markSuspect(String operator, String reason) {
        this.status = ConflictStatus.SUSPECTED;
        this.operator = operator;
        this.resolutionComment = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void dismiss(String operator, String reason) {
        this.status = ConflictStatus.DISMISSED;
        this.operator = operator;
        this.resolutionComment = reason;
        this.updatedAt = LocalDateTime.now();
    }
}
