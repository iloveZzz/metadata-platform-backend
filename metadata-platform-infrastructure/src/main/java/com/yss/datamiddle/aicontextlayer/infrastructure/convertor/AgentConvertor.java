package com.yss.datamiddle.aicontextlayer.infrastructure.convertor;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.Agent;
import com.yss.datamiddle.aicontextlayer.repository.entity.AgentPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * {@link Agent} 领域模型 ↔ {@link AgentPO} 转换（MapStruct）。
 */
@Mapper(config = MapStructInfraConfig.class)
public interface AgentConvertor {

    AgentPO toPO(Agent source);

    Agent toDomain(AgentPO source);

    List<Agent> toDomainList(List<AgentPO> source);
}
