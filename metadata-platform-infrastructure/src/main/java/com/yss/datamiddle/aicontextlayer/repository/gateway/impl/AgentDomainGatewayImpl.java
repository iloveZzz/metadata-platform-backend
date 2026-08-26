package com.yss.datamiddle.aicontextlayer.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.cloud.dto.page.PageQuery;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.AgentDomainGateway;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.AgentDomain;
import com.yss.datamiddle.aicontextlayer.repository.AgentDomainRepository;
import com.yss.datamiddle.aicontextlayer.infrastructure.convertor.AgentDomainConvertor;
import com.yss.datamiddle.aicontextlayer.repository.entity.AgentDomainPO;
import com.yss.datamiddle.aicontextlayer.repository.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AgentDomainGatewayImpl implements AgentDomainGateway {

    private final AgentDomainRepository agentDomainRepository;
    private final AgentDomainConvertor agentDomainConvertor;

    @Override
    public String addAgentDomain(AgentDomain entity) {
        AgentDomainPO po = agentDomainConvertor.toPO(entity);
        agentDomainRepository.insert(po);
        return po.getId();
    }

    @Override
    public boolean updateAgentDomain(AgentDomain entity) {
        return agentDomainRepository.updateById(agentDomainConvertor.toPO(entity)) > 0;
    }

    @Override
    public boolean deleteAgentDomain(String id) {
        return agentDomainRepository.deleteById(id) > 0;
    }

    @Override
    public Optional<AgentDomain> getAgentDomainById(String id) {
        return Optional.ofNullable(agentDomainRepository.selectById(id)).map(this::toDomain);
    }

    @Override
    public PageResult<AgentDomain> pageAgentDomain(PageQuery query) {
        LambdaQueryWrapper<AgentDomainPO> wrapper = Wrappers.lambdaQuery(AgentDomainPO.class);
        
        IPage<AgentDomainPO> result = agentDomainRepository.selectPage(PageUtil.page(query), wrapper);
        List<AgentDomain> records = result.getRecords().stream().map(this::toDomain).collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getSize(), result.getCurrent());
    }

    private AgentDomain toDomain(AgentDomainPO source) {
        return agentDomainConvertor.toDomain(source);
    }
}
