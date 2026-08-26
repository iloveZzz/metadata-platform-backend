package com.yss.datamiddle.semantic.infrastructure.repository.gateway.impl;

import com.yss.datamiddle.semantic.audit.AuditLogEntry;
import com.yss.datamiddle.semantic.audit.AuditLogGateway;
import com.yss.datamiddle.semantic.infrastructure.repository.convertor.SemanticAuditLogConvertor;
import com.yss.datamiddle.semantic.infrastructure.repository.mapper.AuditLogMapper;
import com.yss.datamiddle.semantic.infrastructure.repository.po.AuditLogPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审计日志网关实现（audit_log 不可变只追加，与业务写操作同事务）。
 */
@Repository("semanticAuditLogGatewayImpl")
@RequiredArgsConstructor
public class AuditLogGatewayImpl implements AuditLogGateway {

    private final AuditLogMapper auditLogMapper;
    private final SemanticAuditLogConvertor auditLogConvertor;

    @Override
    public void append(AuditLogEntry entry) {
        auditLogMapper.insert(auditLogConvertor.toPO(entry));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void appendDenied(AuditLogEntry entry) {
        // 被拒操作审计独立事务提交（CT-10）：业务事务回滚不影响被拒审计留痕
        auditLogMapper.insert(auditLogConvertor.toPO(entry));
    }
}
