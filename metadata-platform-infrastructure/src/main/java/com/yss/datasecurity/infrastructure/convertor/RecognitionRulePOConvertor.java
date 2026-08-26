package com.yss.datasecurity.infrastructure.convertor;

import com.yss.datasecurity.domain.model.RecognitionRule;
import com.yss.datasecurity.repository.entity.RecognitionRulePO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = MapStructInfraConfig.class)
public interface RecognitionRulePOConvertor {
    RecognitionRule toDomain(RecognitionRulePO po);
    List<RecognitionRule> toDomainList(List<RecognitionRulePO> poList);
    RecognitionRulePO toPO(RecognitionRule domain);
}
