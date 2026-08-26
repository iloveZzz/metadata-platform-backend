package com.yss.datasecurity.application.convertor;

import com.yss.datasecurity.application.dto.SensitiveRecordVO;
import com.yss.datasecurity.domain.model.SensitiveTaggingRecord;
import com.yss.metadata.application.config.MapStructAppConfig;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = MapStructAppConfig.class)
public interface SensitiveRecordConvertor {

    SensitiveRecordVO toRecordVO(SensitiveTaggingRecord domain);
    List<SensitiveRecordVO> toRecordVOList(List<SensitiveTaggingRecord> domainList);
}
