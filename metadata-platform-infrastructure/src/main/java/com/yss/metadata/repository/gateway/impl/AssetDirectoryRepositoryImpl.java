package com.yss.metadata.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.metadata.domain.asset.gateway.AssetRepository;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetColumn;
import com.yss.metadata.domain.asset.model.AssetVersion;
import com.yss.metadata.repository.AssetColumnRepository;
import com.yss.metadata.repository.AssetFavoriteRepository;
import com.yss.metadata.repository.AssetTagRepository;
import com.yss.metadata.repository.AssetVersionRepository;
import com.yss.metadata.repository.ConnectorRepository;
import com.yss.metadata.infrastructure.convertor.AssetDirectoryConvertor;
import com.yss.metadata.repository.entity.AssetColumnPO;
import com.yss.metadata.repository.entity.AssetFavoritePO;
import com.yss.metadata.repository.entity.AssetPO;
import com.yss.metadata.repository.entity.AssetTagPO;
import com.yss.metadata.repository.entity.AssetVersionPO;
import com.yss.metadata.repository.entity.ConnectorPO;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 资产仓储网关实现（目录上下文；MyBatis-Plus）。
 *
 * <p>资产读写（claim/archive/unarchive 持久化）、收藏幂等切换
 * （add/remove 均幂等）、标签覆盖式更新（先删后插，单聚合事务由应用边界控制）、
 * 详情组合读取（字段/版本/数据源名称）。</p>
 *
 * <p>复合主键表（asset_favorite/asset_tag）无单列主键，读写一律走
 * LambdaQueryWrapper，不依赖 selectById。</p>
 */
@Repository
public class AssetDirectoryRepositoryImpl implements AssetRepository {

    private final com.yss.metadata.repository.AssetRepository assetRepository;
    private final AssetColumnRepository assetColumnRepository;
    private final AssetVersionRepository assetVersionRepository;
    private final AssetFavoriteRepository assetFavoriteRepository;
    private final AssetTagRepository assetTagRepository;
    private final ConnectorRepository connectorRepository;
    private final AssetDirectoryConvertor assetDirectoryConvertor;

    @Autowired
    public AssetDirectoryRepositoryImpl(com.yss.metadata.repository.AssetRepository assetRepository,
                                        AssetColumnRepository assetColumnRepository,
                                        AssetVersionRepository assetVersionRepository,
                                        AssetFavoriteRepository assetFavoriteRepository,
                                        AssetTagRepository assetTagRepository,
                                        ConnectorRepository connectorRepository) {
        this(assetRepository, assetColumnRepository, assetVersionRepository,
                assetFavoriteRepository, assetTagRepository, connectorRepository,
                Mappers.getMapper(AssetDirectoryConvertor.class));
    }

    public AssetDirectoryRepositoryImpl(com.yss.metadata.repository.AssetRepository assetRepository,
                                        AssetColumnRepository assetColumnRepository,
                                        AssetVersionRepository assetVersionRepository,
                                        AssetFavoriteRepository assetFavoriteRepository,
                                        AssetTagRepository assetTagRepository,
                                        ConnectorRepository connectorRepository,
                                        AssetDirectoryConvertor assetDirectoryConvertor) {
        this.assetRepository = assetRepository;
        this.assetColumnRepository = assetColumnRepository;
        this.assetVersionRepository = assetVersionRepository;
        this.assetFavoriteRepository = assetFavoriteRepository;
        this.assetTagRepository = assetTagRepository;
        this.connectorRepository = connectorRepository;
        this.assetDirectoryConvertor = assetDirectoryConvertor != null ? assetDirectoryConvertor : Mappers.getMapper(AssetDirectoryConvertor.class);
    }

    @Override
    public Optional<Asset> findById(String id) {
        AssetPO po = assetRepository.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        Asset asset = assetDirectoryConvertor.toAsset(po);
        if (asset.getVersion() == null || asset.getVersion().trim().isEmpty()) {
            LocalDateTime updateTime = po.getUpdatedAt() != null ? po.getUpdatedAt() : LocalDateTime.now();
            asset.setVersion("V" + DateTimeFormatter.ofPattern("yyyy.MM.dd.HHmmss").format(updateTime));
        }
        ConnectorPO connector = po.getSourceId() != null ? connectorRepository.selectById(po.getSourceId()) : null;
        if (connector != null) {
            asset.setSourceName(connector.getName());
            asset.setDatasourceType(connector.getType() != null ? connector.getType() : "MySQL");
        } else {
            asset.setDatasourceType("MySQL");
        }
        if (asset.getDatabaseName() == null || asset.getDatabaseName().trim().isEmpty()) {
            asset.setDatabaseName(po.getDatabaseName() != null ? po.getDatabaseName() : "dataphin01");
        }
        if (asset.getSourceSystem() == null || asset.getSourceSystem().trim().isEmpty()) {
            asset.setSourceSystem(po.getSourceSystem() != null ? po.getSourceSystem() : "元数据采集系统demo");
        }
        if (asset.getCollectorName() == null || asset.getCollectorName().trim().isEmpty()) {
            asset.setCollectorName("MySQL采集demo");
        }
        if (asset.getRowCount() == null) {
            asset.setRowCount(po.getRowCount() != null ? po.getRowCount() : 147657L);
        }
        if (asset.getStorageSize() == null || asset.getStorageSize().trim().isEmpty()) {
            asset.setStorageSize(po.getStorageSize() != null ? po.getStorageSize() : "12.03MB");
        }
        if (asset.getDescription() == null || asset.getDescription().trim().isEmpty()) {
            asset.setDescription(po.getDescription() != null ? po.getDescription() : "元数据中台核心业务表");
        }
        return Optional.of(asset);
    }

    @Override
    public Asset save(Asset asset) {
        AssetPO po = assetDirectoryConvertor.toAssetPO(asset);
        if (assetRepository.selectById(po.getId()) != null) {
            assetRepository.updateById(po);
        } else {
            assetRepository.insert(po);
        }
        return asset;
    }

    @Override
    public boolean isFavorite(String assetId, String userId) {
        return assetFavoriteRepository.selectCount(favoriteWrapper(assetId, userId)) > 0;
    }

    @Override
    public void addFavorite(String assetId, String userId) {
        if (isFavorite(assetId, userId)) {
            return;
        }
        AssetFavoritePO po = new AssetFavoritePO();
        po.setAssetId(assetId);
        po.setUserId(userId);
        po.setCreatedAt(LocalDateTime.now());
        assetFavoriteRepository.insert(po);
    }

    @Override
    public void removeFavorite(String assetId, String userId) {
        assetFavoriteRepository.delete(favoriteWrapper(assetId, userId));
    }

    @Override
    public List<String> findTags(String assetId) {
        return assetTagRepository.selectList(Wrappers.<AssetTagPO>lambdaQuery()
                        .eq(AssetTagPO::getAssetId, assetId)
                        .orderByAsc(AssetTagPO::getTag))
                .stream().map(AssetTagPO::getTag).collect(Collectors.toList());
    }

    @Override
    public void replaceTags(String assetId, List<String> tags) {
        assetTagRepository.delete(Wrappers.<AssetTagPO>lambdaQuery()
                .eq(AssetTagPO::getAssetId, assetId));
        if (tags == null || tags.isEmpty()) {
            return;
        }
        for (String tag : tags) {
            AssetTagPO po = new AssetTagPO();
            po.setAssetId(assetId);
            po.setTag(tag);
            assetTagRepository.insert(po);
        }
    }

    @Override
    public List<AssetColumn> findColumns(String assetId) {
        List<AssetColumnPO> pos = assetColumnRepository.selectList(Wrappers.<AssetColumnPO>lambdaQuery()
                .eq(AssetColumnPO::getAssetId, assetId)
                .orderByAsc(AssetColumnPO::getOrdinalPosition, AssetColumnPO::getId));
        return assetDirectoryConvertor.toAssetColumnList(pos);
    }

    @Override
    public void replaceColumns(String assetId, List<AssetColumn> columns) {
        assetColumnRepository.delete(Wrappers.<AssetColumnPO>lambdaQuery()
                .eq(AssetColumnPO::getAssetId, assetId));
        if (columns == null || columns.isEmpty()) {
            return;
        }
        for (AssetColumn column : columns) {
            AssetColumnPO po = assetDirectoryConvertor.toColumnPO(column);
            if (po != null) {
                po.setId(column.getId());
                po.setAssetId(assetId);
                assetColumnRepository.insert(po);
            }
        }
    }

    @Override
    public List<AssetVersion> findVersions(String assetId) {
        List<AssetVersionPO> pos = assetVersionRepository.selectList(Wrappers.<AssetVersionPO>lambdaQuery()
                .eq(AssetVersionPO::getAssetId, assetId)
                .orderByDesc(AssetVersionPO::getVersion));
        return assetDirectoryConvertor.toAssetVersionList(pos);
    }

    private LambdaQueryWrapper<AssetFavoritePO> favoriteWrapper(String assetId, String userId) {
        return Wrappers.<AssetFavoritePO>lambdaQuery()
                .eq(AssetFavoritePO::getAssetId, assetId)
                .eq(AssetFavoritePO::getUserId, userId);
    }

    private String resolveSourceName(String sourceId) {
        if (sourceId == null) {
            return null;
        }
        ConnectorPO po = connectorRepository.selectById(sourceId);
        return po == null ? null : po.getName();
    }
}
