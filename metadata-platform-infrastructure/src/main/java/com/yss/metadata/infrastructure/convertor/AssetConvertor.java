package com.yss.metadata.infrastructure.convertor;

import com.yss.metadata.domain.collector.model.CollectedAsset;
import com.yss.metadata.domain.collector.model.CollectedColumn;
import com.yss.metadata.repository.entity.AssetColumnPO;
import com.yss.metadata.repository.entity.AssetPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 采集产物 → 资产持久化对象转换器（MapStruct）。
 *
 * <p>id / sourceId / status / updatedAt 等生命周期字段由 GatewayImpl 用例补充；
 * 禁止 BeanUtils.copyProperties 或手写字段映射。</p>
 */
@Mapper(config = MapStructInfraConfig.class)
public interface AssetConvertor {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sourceId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "taintStatus", ignore = true)
    @Mapping(target = "isExcluded", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AssetPO toPO(CollectedAsset asset);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assetId", ignore = true)
    AssetColumnPO toColumnPO(CollectedColumn column);
}
