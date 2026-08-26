package com.yss.datamiddle.semantic.metric.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 指标口径不可变历史快照（SL-002 / SL-006）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricVersion implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long metricId;
    private Integer versionNo;
    private String expression;
    private String logicDescription;
    @Builder.Default
    private List<String> dimensions = new ArrayList<>();
    private String filters;
    private Integer rollbackFromNo;
    private String createdBy;
    private LocalDateTime createdAt;
}
