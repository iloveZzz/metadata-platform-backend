package com.yss.metadata.infrastructure.convertor;

import com.yss.metadata.domain.integration.model.OpenLineageEventRecord;
import com.yss.metadata.domain.integration.model.OpenLineageParseStatus;
import com.yss.metadata.repository.entity.OpenLineageEventPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;

/**
 * OpenLineage 事件记录持久化转换器（MapStruct；Domain ↔ PO）。
 */
@Mapper(config = MapStructInfraConfig.class)
public interface OpenLineageEventConvertor {

    OpenLineageEventPO toPO(OpenLineageEventRecord record);

    OpenLineageEventRecord toDomain(OpenLineageEventPO po);

    default String mapStatus(OpenLineageParseStatus status) {
        return status == null ? null : status.getValue();
    }

    default OpenLineageParseStatus mapStatus(String value) {
        return OpenLineageParseStatus.fromValue(value);
    }
}
