package com.yss.datamiddle.aicontextlayer.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.cloud.dto.page.PageQuery;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.ToolRegistryGateway;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ToolRegistry;
import com.yss.datamiddle.aicontextlayer.repository.ToolRegistryRepository;
import com.yss.datamiddle.aicontextlayer.infrastructure.convertor.ToolRegistryConvertor;
import com.yss.datamiddle.aicontextlayer.repository.entity.ToolRegistryPO;
import com.yss.datamiddle.aicontextlayer.repository.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ToolRegistryGatewayImpl implements ToolRegistryGateway {

    private final ToolRegistryRepository toolRegistryRepository;
    private final ToolRegistryConvertor toolRegistryConvertor;

    @Override
    public String addToolRegistry(ToolRegistry entity) {
        ToolRegistryPO po = toolRegistryConvertor.toPO(entity);
        toolRegistryRepository.insert(po);
        return po.getToolName();
    }

    @Override
    public boolean updateToolRegistry(ToolRegistry entity) {
        return toolRegistryRepository.updateById(toolRegistryConvertor.toPO(entity)) > 0;
    }

    @Override
    public boolean deleteToolRegistry(String toolName) {
        return toolRegistryRepository.deleteById(toolName) > 0;
    }

    @Override
    public Optional<ToolRegistry> getToolRegistryById(String toolName) {
        return Optional.ofNullable(toolRegistryRepository.selectById(toolName)).map(this::toDomain);
    }

    @Override
    public PageResult<ToolRegistry> pageToolRegistry(PageQuery query) {
        LambdaQueryWrapper<ToolRegistryPO> wrapper = Wrappers.lambdaQuery(ToolRegistryPO.class);
        
        IPage<ToolRegistryPO> result = toolRegistryRepository.selectPage(PageUtil.page(query), wrapper);
        List<ToolRegistry> records = result.getRecords().stream().map(this::toDomain).collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getSize(), result.getCurrent());
    }

    private ToolRegistry toDomain(ToolRegistryPO source) {
        return toolRegistryConvertor.toDomain(source);
    }
}
