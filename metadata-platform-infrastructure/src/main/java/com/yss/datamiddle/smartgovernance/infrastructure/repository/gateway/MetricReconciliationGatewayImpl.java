package com.yss.datamiddle.smartgovernance.infrastructure.repository.gateway;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yss.datamiddle.smartgovernance.domain.metric.gateway.MetricReconciliationGateway;
import com.yss.datamiddle.smartgovernance.domain.metric.model.MetricReconciliationLog;
import com.yss.datamiddle.smartgovernance.infrastructure.repository.mapper.MetricReconciliationLogMapper;
import com.yss.datamiddle.smartgovernance.infrastructure.repository.po.MetricReconciliationLogPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MetricReconciliationGatewayImpl implements MetricReconciliationGateway {

    private final MetricReconciliationLogMapper logMapper;

    @Override
    public void save(MetricReconciliationLog log) {
        logMapper.insert(toPO(log));
    }

    @Override
    public List<MetricReconciliationLog> findByConflictId(String conflictId) {
        LambdaQueryWrapper<MetricReconciliationLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MetricReconciliationLogPO::getConflictId, conflictId)
                .orderByDesc(MetricReconciliationLogPO::getCreatedAt);
        return logMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private MetricReconciliationLog toDomain(MetricReconciliationLogPO po) {
        if (po == null) return null;
        return MetricReconciliationLog.builder()
                .id(po.getId())
                .conflictId(po.getConflictId())
                .canonicalId(po.getCanonicalId())
                .aliasId(po.getAliasId())
                .migratedAssetCount(po.getMigratedAssetCount())
                .operator(po.getOperator())
                .reconcileStrategy(po.getReconcileStrategy())
                .createdAt(po.getCreatedAt())
                .build();
    }

    private MetricReconciliationLogPO toPO(MetricReconciliationLog d) {
        if (d == null) return null;
        return MetricReconciliationLogPO.builder()
                .id(d.getId())
                .conflictId(d.getConflictId())
                .canonicalId(d.getCanonicalId())
                .aliasId(d.getAliasId())
                .migratedAssetCount(d.getMigratedAssetCount())
                .operator(d.getOperator())
                .reconcileStrategy(d.getReconcileStrategy())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
