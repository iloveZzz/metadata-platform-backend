package com.yss.metadata.infrastructure.convertor;

import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetColumn;
import com.yss.metadata.domain.asset.model.AssetStatus;
import com.yss.metadata.domain.asset.model.AssetVersion;
import com.yss.metadata.repository.entity.AssetColumnPO;
import com.yss.metadata.repository.entity.AssetPO;
import com.yss.metadata.repository.entity.AssetVersionPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 资产（目录上下文）持久化转换器（MapStruct）。
 *
 * <p>Domain Asset ↔ AssetPO；状态枚举 ↔ 字符串（列存储值）。
 * sourceName / favorite / tags 为查询组合字段（非持久化），映射忽略；
 * 生命周期字段（id/sourceId/assetId）由用例补充。</p>
 */
@Mapper(config = MapStructInfraConfig.class)
public interface AssetDirectoryConvertor {

    /**
     * 资产 → 持久化对象（查询组合字段 sourceName/favorite/tags 无对应列，天然忽略）。
     */
    AssetPO toAssetPO(Asset asset);

    /**
     * 持久化对象 → 资产（查询组合字段由用例填充）。
     */
    Asset toAsset(AssetPO po);

    /**
     * 资产列表 → 持久化对象列表。
     */
    List<AssetPO> toAssetPOList(List<Asset> assets);

    /**
     * 字段 → 持久化对象（id/assetId 由用例补充）。
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assetId", ignore = true)
    AssetColumnPO toColumnPO(AssetColumn column);

    /**
     * 持久化对象 → 字段。
     */
    AssetColumn toAssetColumn(AssetColumnPO po);

    /**
     * 字段列表 → 字段（持久化对象列表）。
     */
    List<AssetColumn> toAssetColumnList(List<AssetColumnPO> pos);

    /**
     * 持久化对象 → 版本。
     */
    AssetVersion toAssetVersion(AssetVersionPO po);

    /**
     * 版本列表 → 版本。
     */
    List<AssetVersion> toAssetVersionList(List<AssetVersionPO> pos);

    default String toStatusValue(AssetStatus status) {
        return status == null ? null : status.getValue();
    }

    default AssetStatus toStatus(String value) {
        return AssetStatus.fromValue(value);
    }
}
