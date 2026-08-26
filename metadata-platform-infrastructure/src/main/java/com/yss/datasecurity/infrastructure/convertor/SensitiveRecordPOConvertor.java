package com.yss.datasecurity.infrastructure.convertor;

import com.yss.datasecurity.domain.model.SensitiveTaggingRecord;
import com.yss.datasecurity.repository.entity.SensitiveTaggingRecordPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = MapStructInfraConfig.class)
public interface SensitiveRecordPOConvertor {

    SensitiveTaggingRecord toDomain(SensitiveTaggingRecordPO po);
    SensitiveTaggingRecordPO toPO(SensitiveTaggingRecord domain);
    List<SensitiveTaggingRecord> toRecordDomainList(List<SensitiveTaggingRecordPO> poList);
    List<SensitiveTaggingRecordPO> toRecordPOList(List<SensitiveTaggingRecord> domainList);
}
