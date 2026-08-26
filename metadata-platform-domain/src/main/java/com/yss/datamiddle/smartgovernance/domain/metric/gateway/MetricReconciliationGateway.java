package com.yss.datamiddle.smartgovernance.domain.metric.gateway;

import com.yss.datamiddle.smartgovernance.domain.metric.model.MetricReconciliationLog;

import java.util.List;

public interface MetricReconciliationGateway {
    void save(MetricReconciliationLog log);

    List<MetricReconciliationLog> findByConflictId(String conflictId);
}
