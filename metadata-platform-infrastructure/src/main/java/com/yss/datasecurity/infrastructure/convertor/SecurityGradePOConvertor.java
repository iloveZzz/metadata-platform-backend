package com.yss.datasecurity.infrastructure.convertor;

import com.yss.datasecurity.domain.model.SecurityGrade;
import com.yss.datasecurity.repository.entity.SecurityGradePO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = MapStructInfraConfig.class)
public interface SecurityGradePOConvertor {

    SecurityGrade toDomain(SecurityGradePO po);
    List<SecurityGrade> toDomainList(List<SecurityGradePO> poList);

    SecurityGradePO toPO(SecurityGrade domain);
    void updatePOFromDomain(SecurityGrade domain, @MappingTarget SecurityGradePO po);
}
