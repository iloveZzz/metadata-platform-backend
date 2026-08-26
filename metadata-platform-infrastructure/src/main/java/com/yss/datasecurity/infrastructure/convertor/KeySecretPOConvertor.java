package com.yss.datasecurity.infrastructure.convertor;

import com.yss.datasecurity.domain.model.KeySecret;
import com.yss.datasecurity.repository.entity.KeySecretPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = MapStructInfraConfig.class)
public interface KeySecretPOConvertor {

    KeySecret toDomain(KeySecretPO po);
    List<KeySecret> toDomainList(List<KeySecretPO> poList);

    KeySecretPO toPO(KeySecret domain);
}
