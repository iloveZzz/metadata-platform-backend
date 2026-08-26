package com.yss.datamiddle.smartgovernance.domain.metric.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 指标对齐与归并审计日志 (Entity)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricReconciliationLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String conflictId;
    private String canonicalId;
    private String aliasId;
    private Integer migratedAssetCount;
    private String operator;
    private String reconcileStrategy;
    private LocalDateTime createdAt;
}
