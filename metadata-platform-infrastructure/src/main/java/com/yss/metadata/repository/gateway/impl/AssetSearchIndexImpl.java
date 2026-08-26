package com.yss.metadata.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.metadata.client.dto.query.AssetSearchQuery;
import com.yss.metadata.domain.asset.gateway.SearchIndex;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetSearchResult;
import com.yss.metadata.domain.asset.model.AssetSort;
import com.yss.metadata.domain.connector.gateway.ConnectorGateway;
import com.yss.metadata.domain.connector.model.Connector;
import com.yss.metadata.repository.AssetColumnRepository;
import com.yss.metadata.repository.AssetFavoriteRepository;
import com.yss.metadata.repository.ConnectorRepository;
import com.yss.metadata.infrastructure.convertor.AssetDirectoryConvertor;
import com.yss.metadata.repository.entity.AssetColumnPO;
import com.yss.metadata.repository.entity.AssetFavoritePO;
import com.yss.metadata.repository.entity.AssetPO;
import com.yss.metadata.repository.entity.CollectorTaskPO;
import com.yss.metadata.repository.entity.ConnectorPO;
import com.yss.metadata.client.vo.DataSourceSystemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 资产发现索引实现（SearchIndex 可替换 seam；关系库 LIKE 检索）。
 *
 * <p>无 ES/Kafka（数据架构非目标范围）：keyword 命中 asset.name 或
 * asset_column.name（列级命中）时返回资产行；筛选 source（按数据源名称）/
 * type/domain/classification/favorite（当前用户）/mine（owner=当前用户）；
 * sort 枚举 [updatedAt,name,classification]，默认 updatedAt 倒序；分页；
 * 0 条以空分页表达（非错误）。百万级 P95&lt;1s 性能目标随规模评估。</p>
 *
 * <p>当前用户上下文 seam（RBAC slice 06 替换）：favorite/mine 依赖
 * AssetSearchQuery.currentUserId（Web 层解析 X-User-Id，缺省 default-user）。</p>
 */
@Repository
@Slf4j
public class AssetSearchIndexImpl implements SearchIndex {

    private final com.yss.metadata.repository.AssetRepository assetRepository;
    private final AssetColumnRepository assetColumnRepository;
    private final AssetFavoriteRepository assetFavoriteRepository;
    private final ConnectorRepository connectorRepository;
    private final ConnectorGateway connectorGateway;
    private final com.yss.metadata.repository.CollectorTaskRepository collectorTaskRepository;
    private final AssetDirectoryConvertor assetDirectoryConvertor;

    @Autowired
    public AssetSearchIndexImpl(com.yss.metadata.repository.AssetRepository assetRepository,
                                AssetColumnRepository assetColumnRepository,
                                AssetFavoriteRepository assetFavoriteRepository,
                                ConnectorRepository connectorRepository,
                                @Autowired(required = false) ConnectorGateway connectorGateway,
                                com.yss.metadata.repository.CollectorTaskRepository collectorTaskRepository,
                                AssetDirectoryConvertor assetDirectoryConvertor) {
        this.assetRepository = assetRepository;
        this.assetColumnRepository = assetColumnRepository;
        this.assetFavoriteRepository = assetFavoriteRepository;
        this.connectorRepository = connectorRepository;
        this.connectorGateway = connectorGateway;
        this.collectorTaskRepository = collectorTaskRepository;
        this.assetDirectoryConvertor = assetDirectoryConvertor;
    }

    public AssetSearchIndexImpl(com.yss.metadata.repository.AssetRepository assetRepository,
                                AssetColumnRepository assetColumnRepository,
                                AssetFavoriteRepository assetFavoriteRepository,
                                ConnectorRepository connectorRepository,
                                com.yss.metadata.repository.CollectorTaskRepository collectorTaskRepository,
                                AssetDirectoryConvertor assetDirectoryConvertor) {
        this(assetRepository, assetColumnRepository, assetFavoriteRepository, connectorRepository, null, collectorTaskRepository, assetDirectoryConvertor);
    }

    @Override
    public AssetSearchResult search(AssetSearchQuery query) {
        com.github.pagehelper.PageHelper.clearPage();
        LambdaQueryWrapper<AssetPO> wrapper = buildWrapper(query);
        IPage<AssetPO> page = assetRepository.selectPage(
                new Page<>(query.getPageIndex(), query.getPageSize()), wrapper);

        List<AssetPO> records = page.getRecords();
        if (records.isEmpty()) {
            return emptyResult(page.getTotal(), query);
        }
        Map<String, String> sourceNames = loadSourceNames(records);
        Map<String, CollectorTaskPO> collectorTasks = loadCollectorTasks(records);
        Set<String> favoriteAssetIds = loadFavoriteAssetIds(records, query.getCurrentUserId());

        List<Asset> items = new ArrayList<>(records.size());
        for (AssetPO po : records) {
            Asset asset = assetDirectoryConvertor.toAsset(po);
            if (asset.getVersion() == null || asset.getVersion().trim().isEmpty()) {
                LocalDateTime updateTime = po.getUpdatedAt() != null ? po.getUpdatedAt() : LocalDateTime.now();
                asset.setVersion("V" + DateTimeFormatter.ofPattern("yyyy.MM.dd.HHmmss").format(updateTime));
            }
            asset.setSourceName(sourceNames.get(po.getSourceId()));
            asset.setFavorite(favoriteAssetIds.contains(po.getId()));
            if (po.getCollectorTaskId() != null && collectorTasks.containsKey(po.getCollectorTaskId())) {
                CollectorTaskPO task = collectorTasks.get(po.getCollectorTaskId());
                asset.setCollectorName(task.getName());
                asset.setUpdateFrequency(task.getSchedule() != null && !task.getSchedule().equalsIgnoreCase("manual") ? "定时" : "手动");
                asset.setScheduleDescription(task.getSchedule() != null ? task.getSchedule() : "手动执行");
            }
            items.add(asset);
        }
        return AssetSearchResult.builder()
                .items(items)
                .total(page.getTotal())
                .pageIndex(query.getPageIndex())
                .pageSize(query.getPageSize())
                .build();
    }

    /**
     * 搜索条件组装：keyword 列级命中（asset.name LIKE OR asset_column.name LIKE）、
     * 来源（数据源名称 EXISTS）、类型/数据域/分类等值、收藏/我的资产（当前用户）、
     * 数据库筛选、来源系统筛选、已剔除/正常数据隔离。
     */
    private LambdaQueryWrapper<AssetPO> buildWrapper(AssetSearchQuery query) {
        LambdaQueryWrapper<AssetPO> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            List<String> columnHitAssetIds = findAssetIdsByColumnName(keyword);
            if (columnHitAssetIds.isEmpty()) {
                wrapper.like(AssetPO::getName, keyword);
            } else {
                wrapper.and(inner -> inner.like(AssetPO::getName, keyword)
                        .or().in(AssetPO::getId, columnHitAssetIds));
            }
        }
        if (StringUtils.hasText(query.getSourceId())) {
            wrapper.eq(AssetPO::getSourceId, query.getSourceId().trim());
        }
        if (StringUtils.hasText(query.getSource())) {
            String sourceName = query.getSource().trim();
            List<String> matchedSourceIds = new ArrayList<>();
            if (connectorGateway != null) {
                try {
                    List<Connector> all = connectorGateway.findAll();
                    if (all != null) {
                        matchedSourceIds.addAll(all.stream()
                                .filter(c -> sourceName.equalsIgnoreCase(c.getName()) || (c.getName() != null && c.getName().contains(sourceName)))
                                .map(Connector::getId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .collect(Collectors.toList()));
                    }
                } catch (Exception e) {
                    log.warn("ConnectorGateway 获取数据源失败: {}", e.getMessage());
                }
            }
            if (matchedSourceIds.isEmpty()) {
                List<ConnectorPO> localConnectors = connectorRepository.selectList(
                        Wrappers.<ConnectorPO>lambdaQuery().like(ConnectorPO::getName, sourceName));
                if (localConnectors != null && !localConnectors.isEmpty()) {
                    matchedSourceIds.addAll(localConnectors.stream().map(ConnectorPO::getId).collect(Collectors.toList()));
                }
            }
            if (!matchedSourceIds.isEmpty()) {
                if (matchedSourceIds.size() == 1) {
                    wrapper.eq(AssetPO::getSourceId, matchedSourceIds.get(0));
                } else {
                    wrapper.in(AssetPO::getSourceId, matchedSourceIds);
                }
            } else {
                wrapper.eq(AssetPO::getId, "__no_source_hit__");
            }
        }
        if (StringUtils.hasText(query.getDatabase())) {
            wrapper.eq(AssetPO::getDatabaseName, query.getDatabase().trim());
        }
        if (StringUtils.hasText(query.getSourceSystem())) {
            String sys = query.getSourceSystem().trim();
            Set<String> matchedSystemValues = new HashSet<>();
            matchedSystemValues.add(sys);
            if (connectorGateway != null) {
                try {
                    List<DataSourceSystemVO> catalog = connectorGateway.getSystemCatalog();
                    if (catalog != null) {
                        for (DataSourceSystemVO s : catalog) {
                            if (sys.equalsIgnoreCase(s.getCode()) || sys.equalsIgnoreCase(s.getName())
                                    || (s.getName() != null && s.getName().contains(sys))
                                    || (s.getCode() != null && s.getCode().contains(sys))) {
                                if (s.getCode() != null) matchedSystemValues.add(s.getCode());
                                if (s.getName() != null) matchedSystemValues.add(s.getName());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("ConnectorGateway 获取系统名录失败: {}", e.getMessage());
                }
            }
            wrapper.and(w -> {
                w.in(AssetPO::getSourceSystem, matchedSystemValues);
                for (String val : matchedSystemValues) {
                    w.or().like(AssetPO::getSourceSystem, val);
                }
            });
        }
        if (StringUtils.hasText(query.getCollectorTaskId())) {
            wrapper.eq(AssetPO::getCollectorTaskId, query.getCollectorTaskId().trim());
        }
        if (Boolean.TRUE.equals(query.getIsExcluded())) {
            wrapper.eq(AssetPO::getIsExcluded, true);
        } else {
            wrapper.and(w -> w.eq(AssetPO::getIsExcluded, false).or().isNull(AssetPO::getIsExcluded));
        }
        if (StringUtils.hasText(query.getType())) {
            wrapper.eq(AssetPO::getType, query.getType().trim());
        }
        if (StringUtils.hasText(query.getDomain())) {
            wrapper.eq(AssetPO::getDomain, query.getDomain().trim());
        }
        // slice 06 RBAC：数据域过滤（X-User-Domains 头解析；null/空 = 全部放行）
        if (query.getAllowedDomains() != null && !query.getAllowedDomains().isEmpty()) {
            wrapper.in(AssetPO::getDomain, query.getAllowedDomains());
        }
        if (StringUtils.hasText(query.getClassification())) {
            wrapper.eq(AssetPO::getClassification, query.getClassification().trim());
        }
        if (Boolean.TRUE.equals(query.getFavorite())) {
            if (!StringUtils.hasText(query.getCurrentUserId())) {
                // 无当前用户上下文：仅看收藏无命中（空分页）
                wrapper.eq(AssetPO::getId, "__no_favorite_hit__");
            } else {
                wrapper.apply("EXISTS (SELECT 1 FROM asset_favorite f "
                        + "WHERE f.asset_id = asset.id AND f.user_id = {0})", query.getCurrentUserId());
            }
        }
        if (Boolean.TRUE.equals(query.getMine())) {
            wrapper.eq(AssetPO::getOwner, query.getCurrentUserId());
        }
        applySort(wrapper, AssetSort.fromValue(query.getSort()));
        return wrapper;
    }

    /**
     * 列级命中：keyword 命中 asset_column.name 的资产 id 清单。
     */
    private List<String> findAssetIdsByColumnName(String keyword) {
        return assetColumnRepository.selectList(Wrappers.<AssetColumnPO>lambdaQuery()
                        .like(AssetColumnPO::getName, keyword))
                .stream().map(AssetColumnPO::getAssetId)
                .distinct().collect(Collectors.toList());
    }

    /**
     * 数据源名称批量解析（asset.source_id → data_source.name / ConnectorGateway.name）。
     */
    private Map<String, String> loadSourceNames(List<AssetPO> records) {
        List<String> sourceIds = records.stream().map(AssetPO::getSourceId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (sourceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        if (connectorGateway != null) {
            try {
                List<Connector> all = connectorGateway.findAll();
                if (all != null && !all.isEmpty()) {
                    return all.stream()
                            .filter(c -> c.getId() != null && c.getName() != null)
                            .collect(Collectors.toMap(Connector::getId, Connector::getName, (a, b) -> a));
                }
            } catch (Exception e) {
                log.warn("ConnectorGateway loadSourceNames failed: {}", e.getMessage());
            }
        }
        return connectorRepository.selectBatchIds(sourceIds).stream()
                .collect(Collectors.toMap(ConnectorPO::getId, ConnectorPO::getName, (a, b) -> a));
    }

    /**
     * 采集任务批量解析（asset.collector_task_id → collector_task）。
     */
    private Map<String, CollectorTaskPO> loadCollectorTasks(List<AssetPO> records) {
        List<String> taskIds = records.stream().map(AssetPO::getCollectorTaskId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return collectorTaskRepository.selectBatchIds(taskIds).stream()
                .collect(Collectors.toMap(CollectorTaskPO::getId, t -> t, (a, b) -> a));
    }

    /**
     * 当前用户收藏资产 id 集合（页内批量判定 favorite 标志）。
     */
    private Set<String> loadFavoriteAssetIds(List<AssetPO> records, String currentUserId) {
        if (!StringUtils.hasText(currentUserId)) {
            return Collections.emptySet();
        }
        List<String> assetIds = records.stream().map(AssetPO::getId).collect(Collectors.toList());
        return assetFavoriteRepository.selectList(Wrappers.<AssetFavoritePO>lambdaQuery()
                        .eq(AssetFavoritePO::getUserId, currentUserId)
                        .in(AssetFavoritePO::getAssetId, assetIds))
                .stream().map(AssetFavoritePO::getAssetId).collect(Collectors.toSet());
    }

    private void applySort(LambdaQueryWrapper<AssetPO> wrapper, AssetSort sort) {
        switch (sort) {
            case NAME:
                wrapper.orderByAsc(AssetPO::getName);
                break;
            case CLASSIFICATION:
                wrapper.orderByAsc(AssetPO::getClassification);
                break;
            default:
                wrapper.orderByDesc(AssetPO::getUpdatedAt);
                break;
        }
    }

    private AssetSearchResult emptyResult(long total, AssetSearchQuery query) {
        return AssetSearchResult.builder()
                .items(Collections.emptyList())
                .total(total)
                .pageIndex(query.getPageIndex())
                .pageSize(query.getPageSize())
                .build();
    }
}
