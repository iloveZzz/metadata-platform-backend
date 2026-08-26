package com.yss.datamiddle.smartgovernance.domain.metric.gateway;

import com.yss.datamiddle.smartgovernance.domain.metric.model.ConflictStatus;
import com.yss.datamiddle.smartgovernance.domain.metric.model.ConflictType;
import com.yss.datamiddle.smartgovernance.domain.metric.model.MetricConflictRecord;

import java.util.List;
import java.util.Optional;

public interface MetricConflictGateway {
    void save(MetricConflictRecord record);

    void batchSave(List<MetricConflictRecord> records);

    Optional<MetricConflictRecord> findById(String id);

    void update(MetricConflictRecord record);

    List<MetricConflictRecord> queryConflicts(
            Integer pageIndex,
            Integer pageSize,
            ConflictStatus status,
            ConflictType conflictType,
            String keyword
    );

    long countConflicts(ConflictStatus status, ConflictType conflictType, String keyword);
}
