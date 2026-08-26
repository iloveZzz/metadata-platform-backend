package com.yss.metadata.repository.gateway.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.metadata.domain.audit.gateway.AuditLogGateway;
import com.yss.metadata.domain.audit.model.AuditLogEntry;
import com.yss.metadata.domain.audit.model.AuditLogPage;
import com.yss.metadata.repository.AuditLogRepository;
import com.yss.metadata.infrastructure.convertor.AuditLogConvertor;
import com.yss.metadata.repository.entity.AuditLogPO;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审计日志仓储实现（MyBatis-Plus；audit_log 不可变：追加写入 + 只读分页查询）。
 */
@Repository("metadataAuditLogGatewayImpl")
public class AuditLogGatewayImpl implements AuditLogGateway {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogConvertor auditLogConvertor;

    @Autowired
    public AuditLogGatewayImpl(AuditLogRepository auditLogRepository) {
        this(auditLogRepository, Mappers.getMapper(AuditLogConvertor.class));
    }

    public AuditLogGatewayImpl(AuditLogRepository auditLogRepository, AuditLogConvertor auditLogConvertor) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogConvertor = auditLogConvertor != null ? auditLogConvertor : Mappers.getMapper(AuditLogConvertor.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void record(AuditLogEntry entry) {
        auditLogRepository.insert(auditLogConvertor.toPO(entry));
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogPage page(int pageIndex, int pageSize) {
        IPage<AuditLogPO> page = auditLogRepository.selectPage(
                new Page<>(pageIndex, pageSize),
                Wrappers.<AuditLogPO>lambdaQuery().orderByDesc(AuditLogPO::getTime));
        return AuditLogPage.builder()
                .items(auditLogConvertor.toDomainList(page.getRecords()))
                .total(page.getTotal())
                .pageIndex(pageIndex)
                .pageSize(pageSize)
                .build();
    }
}
