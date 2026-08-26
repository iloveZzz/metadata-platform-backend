package com.yss.datamiddle.semantic.metric.batch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricImportResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private int totalCount;
    private int successCount;
    private int failureCount;
    @Builder.Default
    private List<MetricImportError> errors = new ArrayList<>();
}
