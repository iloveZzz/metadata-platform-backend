package com.yss.datamiddle.aicontextlayer.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.cloud.dto.page.PageQuery;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.AgentCredentialGateway;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.AgentCredential;
import com.yss.datamiddle.aicontextlayer.repository.AgentCredentialRepository;
import com.yss.datamiddle.aicontextlayer.infrastructure.convertor.AgentCredentialConvertor;
import com.yss.datamiddle.aicontextlayer.repository.entity.AgentCredentialPO;
import com.yss.datamiddle.aicontextlayer.repository.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AgentCredentialGatewayImpl implements AgentCredentialGateway {

    private final AgentCredentialRepository agentCredentialRepository;
    private final AgentCredentialConvertor agentCredentialConvertor;

    @Override
    public String addAgentCredential(AgentCredential entity) {
        AgentCredentialPO po = agentCredentialConvertor.toPO(entity);
        agentCredentialRepository.insert(po);
        return po.getId();
    }

    @Override
    public boolean updateAgentCredential(AgentCredential entity) {
        return agentCredentialRepository.updateById(agentCredentialConvertor.toPO(entity)) > 0;
    }

    @Override
    public boolean deleteAgentCredential(String id) {
        return agentCredentialRepository.deleteById(id) > 0;
    }

    @Override
    public Optional<AgentCredential> getAgentCredentialById(String id) {
        return Optional.ofNullable(agentCredentialRepository.selectById(id)).map(this::toDomain);
    }

    @Override
    public PageResult<AgentCredential> pageAgentCredential(PageQuery query) {
        LambdaQueryWrapper<AgentCredentialPO> wrapper = Wrappers.lambdaQuery(AgentCredentialPO.class);
        
        IPage<AgentCredentialPO> result = agentCredentialRepository.selectPage(PageUtil.page(query), wrapper);
        List<AgentCredential> records = result.getRecords().stream().map(this::toDomain).collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    public List<AgentCredential> listCredentials() {
        // 凭据校验候选全量加载（含 REVOKED/ROTATED/EXPIRED 行，吊销即时生效识别主体，SEC-05）；
        // MVP 规模（数据架构 §9，Agent ≤50）下全量扫描可接受。
        return agentCredentialRepository.selectList(null).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    private AgentCredential toDomain(AgentCredentialPO source) {
        return agentCredentialConvertor.toDomain(source);
    }
}
