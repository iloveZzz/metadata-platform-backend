package com.yss.metadata.infrastructure.convertor;

import com.yss.metadata.domain.governance.model.Classification;
import com.yss.metadata.domain.governance.model.ClassificationStatus;
import com.yss.metadata.repository.entity.ClassificationPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 分级分类结果持久化转换器（MapStruct；Domain ↔ PO）。
 */
@Mapper(config = MapStructInfraConfig.class)
public interface ClassificationConvertor {

    ClassificationPO toPO(Classification classification);

    Classification toDomain(ClassificationPO po);

    List<Classification> toDomainList(List<ClassificationPO> pos);

    default String mapStatus(ClassificationStatus status) {
        return status == null ? null : status.getValue();
    }

    default ClassificationStatus mapStatus(String value) {
        return ClassificationStatus.fromValue(value);
    }
}
