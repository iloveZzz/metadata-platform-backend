package com.yss.metadata.application.rbac.service.convertor;

import com.yss.metadata.client.vo.AuditLogVO;
import com.yss.metadata.client.vo.RoleVO;
import com.yss.metadata.domain.audit.model.AuditLogEntry;
import com.yss.metadata.domain.rbac.model.RoleSummary;
import com.yss.metadata.application.config.MapStructAppConfig;
import org.mapstruct.Mapper;

/**
 * RBAC 域应用转换器（MapStruct；Domain → VO）。
 */
@Mapper(config = MapStructAppConfig.class)
public interface RbacAppConvertor {

    RoleVO toRoleVO(RoleSummary summary);

    AuditLogVO toAuditLogVO(AuditLogEntry entry);
}
