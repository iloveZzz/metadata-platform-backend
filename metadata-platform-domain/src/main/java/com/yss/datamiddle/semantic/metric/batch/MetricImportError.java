package com.yss.datamiddle.semantic.metric.batch;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricImportError implements Serializable {
    private static final long serialVersionUID = 1L;

    private int rowNumber;
    private String metricName;
    private String errorCode;
    private String errorMessage;
}
