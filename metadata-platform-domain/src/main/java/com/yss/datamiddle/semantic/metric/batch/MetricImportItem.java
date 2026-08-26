package com.yss.datamiddle.semantic.metric.batch;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量导入单条指标条目
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricImportItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private int rowNumber;
    private String name;
    private String metricGroup;
    private String description;
    private String owner;
    private String expression;
    private String logicDescription;
}
