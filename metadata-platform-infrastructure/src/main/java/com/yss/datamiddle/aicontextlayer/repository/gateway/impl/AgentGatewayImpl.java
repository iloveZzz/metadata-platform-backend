package com.yss.datamiddle.aicontextlayer.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.cloud.dto.page.PageQuery;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.AgentGateway;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.Agent;
import com.yss.datamiddle.aicontextlayer.repository.AgentRepository;
import com.yss.datamiddle.aicontextlayer.infrastructure.convertor.AgentConvertor;
import com.yss.datamiddle.aicontextlayer.repository.entity.AgentPO;
import com.yss.datamiddle.aicontextlayer.repository.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AgentGatewayImpl implements AgentGateway {

    private final AgentRepository agentRepository;
    private final AgentConvertor agentConvertor;

    @Override
    public String addAgent(Agent entity) {
        AgentPO po = agentConvertor.toPO(entity);
        agentRepository.insert(po);
        return po.getId();
    }

    @Override
    public boolean updateAgent(Agent entity) {
        return agentRepository.updateById(agentConvertor.toPO(entity)) > 0;
    }

    @Override
    public boolean deleteAgent(String id) {
        return agentRepository.deleteById(id) > 0;
    }

    @Override
    public Optional<Agent> getAgentById(String id) {
        return Optional.ofNullable(agentRepository.selectById(id)).map(this::toDomain);
    }

    @Override
    public PageResult<Agent> pageAgent(PageQuery query) {
        LambdaQueryWrapper<AgentPO> wrapper = Wrappers.lambdaQuery(AgentPO.class);
        
        IPage<AgentPO> result = agentRepository.selectPage(PageUtil.page(query), wrapper);
        List<Agent> records = result.getRecords().stream().map(this::toDomain).collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getSize(), result.getCurrent());
    }

    private Agent toDomain(AgentPO source) {
        return agentConvertor.toDomain(source);
    }
}
