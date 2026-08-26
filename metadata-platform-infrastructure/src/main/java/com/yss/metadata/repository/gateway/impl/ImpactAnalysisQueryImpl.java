package com.yss.metadata.repository.gateway.impl;

import com.yss.metadata.domain.lineage.gateway.ImpactAnalysisRepository;
import com.yss.metadata.domain.lineage.model.ImpactNode;
import com.yss.metadata.repository.LineageImpactMapper;
import com.yss.metadata.infrastructure.convertor.ImpactHitConvertor;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 影响分析查询实现（递归 CTE 原生 SQL，收敛于 {@link LineageImpactMapper}；
 * 人工确认项）。下游全量召回 + 环保护（路径内边去重）+ 深度上限；
 * 0 影响返回空列表（非错误）。
 */
@Repository
public class ImpactAnalysisQueryImpl implements ImpactAnalysisRepository {

    private final LineageImpactMapper lineageImpactMapper;
    private final ImpactHitConvertor impactHitConvertor;

    @Autowired
    public ImpactAnalysisQueryImpl(LineageImpactMapper lineageImpactMapper) {
        this(lineageImpactMapper, Mappers.getMapper(ImpactHitConvertor.class));
    }

    public ImpactAnalysisQueryImpl(LineageImpactMapper lineageImpactMapper, ImpactHitConvertor impactHitConvertor) {
        this.lineageImpactMapper = lineageImpactMapper;
        this.impactHitConvertor = impactHitConvertor != null ? impactHitConvertor : Mappers.getMapper(ImpactHitConvertor.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImpactNode> findDownstream(String assetId, int maxDepth) {
        return impactHitConvertor.toDomainList(
                lineageImpactMapper.selectDownstream(assetId, maxDepth));
    }
}
