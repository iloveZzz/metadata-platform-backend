package com.yss.datasecurity.infrastructure.convertor;

import com.yss.datasecurity.domain.model.KeyTaskReference;
import com.yss.datasecurity.repository.entity.KeyTaskReferencePO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = MapStructInfraConfig.class)
public interface KeyTaskReferencePOConvertor {
    KeyTaskReference toDomain(KeyTaskReferencePO po);
    List<KeyTaskReference> toDomainList(List<KeyTaskReferencePO> poList);
    KeyTaskReferencePO toPO(KeyTaskReference domain);
}
