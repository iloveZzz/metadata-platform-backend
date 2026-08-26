package com.yss.datamiddle.smartgovernance.infrastructure.repository.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sg_metric_conflict")
public class MetricConflictPO {
    @TableId
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
    private String conflictType;
    private BigDecimal similarityScore;
    private String formulaA;
    private String formulaB;
    private String astDiffSummary;
    private String status;
    private String canonicalId;
    private String resolutionComment;
    private String operator;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
