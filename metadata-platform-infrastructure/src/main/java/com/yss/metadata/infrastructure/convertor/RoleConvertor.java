package com.yss.metadata.infrastructure.convertor;

import com.yss.metadata.domain.rbac.model.Role;
import com.yss.metadata.repository.entity.RolePO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;

/**
 * 角色持久化转换器（MapStruct；Domain ↔ PO）。
 */
@Mapper(config = MapStructInfraConfig.class)
public interface RoleConvertor {

    RolePO toPO(Role role);

    Role toDomain(RolePO po);
}
