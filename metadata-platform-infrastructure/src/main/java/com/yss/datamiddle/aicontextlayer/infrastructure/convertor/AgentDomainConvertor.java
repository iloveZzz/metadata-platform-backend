package com.yss.datamiddle.aicontextlayer.infrastructure.convertor;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.AgentDomain;
import com.yss.datamiddle.aicontextlayer.repository.entity.AgentDomainPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * {@link AgentDomain} 领域模型 ↔ {@link AgentDomainPO} 转换（MapStruct）。
 */
@Mapper(config = MapStructInfraConfig.class)
public interface AgentDomainConvertor {

    AgentDomainPO toPO(AgentDomain source);

    AgentDomain toDomain(AgentDomainPO source);

    List<AgentDomain> toDomainList(List<AgentDomainPO> source);
}
