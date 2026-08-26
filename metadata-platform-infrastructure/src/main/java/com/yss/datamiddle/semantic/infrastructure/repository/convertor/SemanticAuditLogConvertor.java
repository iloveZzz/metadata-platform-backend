package com.yss.datamiddle.semantic.infrastructure.repository.convertor;

import com.yss.datamiddle.semantic.audit.AuditLogEntry;
import com.yss.datamiddle.semantic.infrastructure.repository.po.AuditLogPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;

/**
 * 审计 PO ↔ 领域值对象 Convertor（MapStruct，Spring 组件模型）。
 */
@Mapper(config = MapStructInfraConfig.class)
public interface SemanticAuditLogConvertor {

    AuditLogPO toPO(AuditLogEntry entry);
}
