package com.yss.metadata.infrastructure.convertor;

import com.yss.metadata.domain.lineage.model.ImpactNode;
import com.yss.metadata.repository.entity.ImpactHitPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 影响分析查询行 → 领域节点转换器（MapStruct）。
 */
@Mapper(config = MapStructInfraConfig.class)
public interface ImpactHitConvertor {

    ImpactNode toDomain(ImpactHitPO row);

    List<ImpactNode> toDomainList(List<ImpactHitPO> rows);
}
