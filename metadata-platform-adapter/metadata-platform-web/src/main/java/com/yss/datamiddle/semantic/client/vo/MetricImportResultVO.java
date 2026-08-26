package com.yss.datamiddle.semantic.client.vo;

import com.yss.datamiddle.semantic.metric.batch.MetricImportError;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricImportResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer totalCount;
    private Integer successCount;
    private Integer failureCount;
    private List<MetricImportError> errors;
}
