package com.yss.metadata.infrastructure.convertor;

import com.yss.metadata.domain.integration.model.IntegrationConfig;
import com.yss.metadata.repository.entity.IntegrationConfigPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;

/**
 * 集成配置持久化转换器（MapStruct；Domain ↔ PO）。
 */
@Mapper(config = MapStructInfraConfig.class)
public interface IntegrationConfigConvertor {

    IntegrationConfigPO toPO(IntegrationConfig config);

    IntegrationConfig toDomain(IntegrationConfigPO po);
}
