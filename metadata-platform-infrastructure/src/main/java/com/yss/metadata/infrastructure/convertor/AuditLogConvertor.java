package com.yss.metadata.infrastructure.convertor;

import com.yss.metadata.domain.audit.model.AuditLogEntry;
import com.yss.metadata.repository.entity.AuditLogPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 审计日志持久化转换器（MapStruct；Domain ↔ PO）。
 */
@Mapper(config = MapStructInfraConfig.class)
public interface AuditLogConvertor {

    AuditLogPO toPO(AuditLogEntry entry);

    AuditLogEntry toDomain(AuditLogPO po);

    List<AuditLogEntry> toDomainList(List<AuditLogPO> pos);
}
