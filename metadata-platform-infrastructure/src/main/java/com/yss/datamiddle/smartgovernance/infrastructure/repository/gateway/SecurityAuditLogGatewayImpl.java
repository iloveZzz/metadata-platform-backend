package com.yss.datamiddle.smartgovernance.infrastructure.repository.gateway;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.datamiddle.smartgovernance.domain.security.gateway.SecurityAuditLogGateway;
import com.yss.datamiddle.smartgovernance.domain.security.model.SecurityAuditLog;
import com.yss.datamiddle.smartgovernance.infrastructure.repository.mapper.SecurityAuditLogMapper;
import com.yss.datamiddle.smartgovernance.infrastructure.repository.po.SecurityAuditLogPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SecurityAuditLogGatewayImpl implements SecurityAuditLogGateway {

    private final SecurityAuditLogMapper auditLogMapper;

    @Override
    public void save(SecurityAuditLog log) {
        auditLogMapper.insert(toPO(log));
    }

    @Override
    public void batchSave(List<SecurityAuditLog> logs) {
        if (logs != null && !logs.isEmpty()) {
            for (SecurityAuditLog l : logs) {
                auditLogMapper.insert(toPO(l));
            }
        }
    }

    @Override
    public Optional<SecurityAuditLog> findById(String id) {
        return Optional.ofNullable(auditLogMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<SecurityAuditLog> queryLogs(Integer pageIndex, Integer pageSize, String keyword) {
        LambdaQueryWrapper<SecurityAuditLogPO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim();
            wrapper.like(SecurityAuditLogPO::getColumnName, k)
                    .or().like(SecurityAuditLogPO::getTableName, k)
                    .or().like(SecurityAuditLogPO::getReason, k)
                    .or().like(SecurityAuditLogPO::getOperator, k);
        }
        wrapper.orderByDesc(SecurityAuditLogPO::getCreatedAt);

        int current = (pageIndex != null && pageIndex > 0) ? pageIndex : 1;
        int size = (pageSize != null && pageSize > 0) ? pageSize : 20;

        Page<SecurityAuditLogPO> page = new Page<>(current, size);
        return auditLogMapper.selectPage(page, wrapper).getRecords().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countLogs(String keyword) {
        LambdaQueryWrapper<SecurityAuditLogPO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim();
            wrapper.like(SecurityAuditLogPO::getColumnName, k)
                    .or().like(SecurityAuditLogPO::getTableName, k)
                    .or().like(SecurityAuditLogPO::getReason, k)
                    .or().like(SecurityAuditLogPO::getOperator, k);
        }
        return auditLogMapper.selectCount(wrapper);
    }

    private SecurityAuditLog toDomain(SecurityAuditLogPO po) {
        if (po == null) return null;
        return SecurityAuditLog.builder()
                .id(po.getId())
                .candidateId(po.getCandidateId())
                .dataSource(po.getDataSource())
                .databaseName(po.getDatabaseName())
                .tableName(po.getTableName())
                .columnName(po.getColumnName())
                .previousLevel(po.getPreviousLevel())
                .newLevel(po.getNewLevel())
                .actionType(po.getActionType())
                .operator(po.getOperator())
                .reason(po.getReason())
                .createdAt(po.getCreatedAt())
                .build();
    }

    private SecurityAuditLogPO toPO(SecurityAuditLog d) {
        if (d == null) return null;
        return SecurityAuditLogPO.builder()
                .id(d.getId())
                .candidateId(d.getCandidateId())
                .dataSource(d.getDataSource())
                .databaseName(d.getDatabaseName())
                .tableName(d.getTableName())
                .columnName(d.getColumnName())
                .previousLevel(d.getPreviousLevel())
                .newLevel(d.getNewLevel())
                .actionType(d.getActionType())
                .operator(d.getOperator())
                .reason(d.getReason())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
