package com.yss.datamiddle.aicontextlayer.infrastructure.convertor;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.AuditLog;
import com.yss.datamiddle.aicontextlayer.repository.entity.AuditLogPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * {@link AuditLog} 领域模型 ↔ {@link AuditLogPO} 转换（MapStruct）。
 */
@Mapper(config = MapStructInfraConfig.class)
public interface AclAuditLogConvertor {

    AuditLogPO toPO(AuditLog source);

    AuditLog toDomain(AuditLogPO source);

    List<AuditLog> toDomainList(List<AuditLogPO> source);
}
