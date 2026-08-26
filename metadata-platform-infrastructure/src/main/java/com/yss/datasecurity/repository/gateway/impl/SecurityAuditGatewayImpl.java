package com.yss.datasecurity.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.datasecurity.domain.gateway.SecurityAuditGateway;
import com.yss.datasecurity.domain.model.SecurityAuditLog;
import com.yss.datasecurity.repository.entity.SecurityAuditLogPO;
import com.yss.datasecurity.repository.mapper.SecurityAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SecurityAuditGatewayImpl implements SecurityAuditGateway {

    private final SecurityAuditLogRepository repository;

    @Override
    public Long save(SecurityAuditLog log) {
        SecurityAuditLogPO po = toPO(log);
        repository.insert(po);
        log.setId(po.getId());
        return po.getId();
    }

    @Override
    public List<SecurityAuditLog> findPage(int pageIndex, int pageSize, String actionType, String operator, String riskLevel) {
        LambdaQueryWrapper<SecurityAuditLogPO> wrapper = new LambdaQueryWrapper<SecurityAuditLogPO>()
                .eq(StringUtils.hasText(actionType), SecurityAuditLogPO::getActionType, actionType)
                .eq(StringUtils.hasText(operator), SecurityAuditLogPO::getOperator, operator)
                .eq(StringUtils.hasText(riskLevel), SecurityAuditLogPO::getRiskLevel, riskLevel)
                .orderByDesc(SecurityAuditLogPO::getId);
        Page<SecurityAuditLogPO> page = repository.selectPage(new Page<>(pageIndex, pageSize), wrapper);
        return page.getRecords().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countPage(String actionType, String operator, String riskLevel) {
        LambdaQueryWrapper<SecurityAuditLogPO> wrapper = new LambdaQueryWrapper<SecurityAuditLogPO>()
                .eq(StringUtils.hasText(actionType), SecurityAuditLogPO::getActionType, actionType)
                .eq(StringUtils.hasText(operator), SecurityAuditLogPO::getOperator, operator)
                .eq(StringUtils.hasText(riskLevel), SecurityAuditLogPO::getRiskLevel, riskLevel);
        return repository.selectCount(wrapper);
    }

    private SecurityAuditLogPO toPO(SecurityAuditLog d) {
        return SecurityAuditLogPO.builder()
                .id(d.getId())
                .actionType(d.getActionType())
                .operator(d.getOperator())
                .clientIp(d.getClientIp())
                .targetResource(d.getTargetResource())
                .riskLevel(d.getRiskLevel())
                .actionDetail(d.getActionDetail())
                .createdAt(d.getCreatedAt())
                .build();
    }

    private SecurityAuditLog toDomain(SecurityAuditLogPO po) {
        return SecurityAuditLog.builder()
                .id(po.getId())
                .actionType(po.getActionType())
                .operator(po.getOperator())
                .clientIp(po.getClientIp())
                .targetResource(po.getTargetResource())
                .riskLevel(po.getRiskLevel())
                .actionDetail(po.getActionDetail())
                .createdAt(po.getCreatedAt())
                .build();
    }
}
