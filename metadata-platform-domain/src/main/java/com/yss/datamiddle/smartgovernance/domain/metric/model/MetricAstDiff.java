package com.yss.datamiddle.smartgovernance.domain.metric.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 指标 AST 差异对比值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricAstDiff implements Serializable {
    private static final long serialVersionUID = 1L;

    private ConflictType conflictType;
    private Double similarityScore;
    private Boolean aggMatch;
    private String whereClauseDiff;
    private String astSummary;
}
