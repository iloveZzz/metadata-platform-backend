package com.yss.datasecurity.infrastructure.convertor;

import com.yss.datasecurity.domain.model.KeyPermission;
import com.yss.datasecurity.repository.entity.KeyPermissionPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = MapStructInfraConfig.class)
public interface KeyPermissionPOConvertor {
    KeyPermission toDomain(KeyPermissionPO po);
    List<KeyPermission> toDomainList(List<KeyPermissionPO> poList);
    KeyPermissionPO toPO(KeyPermission domain);
}
