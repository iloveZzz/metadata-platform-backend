package com.yss.datamiddle.smartgovernance.web.vo;

import com.yss.datamiddle.smartgovernance.domain.metric.model.MetricAstDiff;
import com.yss.datamiddle.smartgovernance.domain.metric.model.MetricConflictRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricConflictDiffVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private MetricConflictRecord conflict;
    private Map<String, Object> indicatorA;
    private Map<String, Object> indicatorB;
    private MetricAstDiff astDiff;
}
