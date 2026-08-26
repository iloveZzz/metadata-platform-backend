package com.yss.datamiddle.aicontextlayer.infrastructure.convertor;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ToolRegistry;
import com.yss.datamiddle.aicontextlayer.repository.entity.ToolRegistryPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * {@link ToolRegistry} 领域模型 ↔ {@link ToolRegistryPO} 转换（MapStruct）。
 */
@Mapper(config = MapStructInfraConfig.class)
public interface ToolRegistryConvertor {

    ToolRegistryPO toPO(ToolRegistry source);

    ToolRegistry toDomain(ToolRegistryPO source);

    List<ToolRegistry> toDomainList(List<ToolRegistryPO> source);
}
