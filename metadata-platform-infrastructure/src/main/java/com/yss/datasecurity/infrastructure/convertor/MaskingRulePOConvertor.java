package com.yss.datasecurity.infrastructure.convertor;

import com.yss.datasecurity.domain.model.MaskingRule;
import com.yss.datasecurity.repository.entity.MaskingRulePO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = MapStructInfraConfig.class)
public interface MaskingRulePOConvertor {

    MaskingRule toDomain(MaskingRulePO po);
    List<MaskingRule> toDomainList(List<MaskingRulePO> poList);

    MaskingRulePO toPO(MaskingRule domain);
}
