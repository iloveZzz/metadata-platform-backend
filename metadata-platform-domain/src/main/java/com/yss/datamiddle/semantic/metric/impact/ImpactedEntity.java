package com.yss.datamiddle.semantic.metric.impact;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 受影响的下游实体（指标、资产等）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImpactedEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long entityId;
    private String entityType; // "METRIC", "ASSET", "REPORT"
    private String entityName;
    private String owner;
    private String impactDescription;
}
