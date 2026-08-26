package com.yss.metadata.application.asset.support;

import com.yss.metadata.domain.asset.gateway.AssetRepository;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetColumn;
import com.yss.metadata.domain.asset.model.AssetVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 资产仓储内存实现（测试 seam）。
 *
 * <p>应用/契约测试替身，替代真实数据库持久化；与 {@link InMemorySearchIndex}
 * 共享同一内存存储（同一实例注入）。</p>
 */
public class InMemoryAssetRepository implements AssetRepository {

    private final Map<String, Asset> store = new ConcurrentHashMap<>();

    private final Map<String, Set<String>> favoriteUsersByAsset = new ConcurrentHashMap<>();

    private final Map<String, Set<String>> tagsByAsset = new ConcurrentHashMap<>();

    private final Map<String, List<AssetColumn>> columnsByAsset = new ConcurrentHashMap<>();

    private final Map<String, List<AssetVersion>> versionsByAsset = new ConcurrentHashMap<>();

    private final Map<String, String> sourceNames = new ConcurrentHashMap<>();

    // ---------- 种子辅助（契约/应用测试用） ----------

    public void seedSourceName(String sourceId, String name) {
        sourceNames.put(sourceId, name);
    }

    public void seed(Asset asset) {
        asset.setSourceName(sourceNames.get(asset.getSourceId()));
        store.put(asset.getId(), asset);
    }

    public void seedColumns(String assetId, List<AssetColumn> columns) {
        columnsByAsset.put(assetId, new ArrayList<>(columns));
    }

    public void seedVersions(String assetId, List<AssetVersion> versions) {
        versionsByAsset.put(assetId, new ArrayList<>(versions));
    }

    public void seedFavorite(String assetId, String userId) {
        favoriteUsersByAsset.computeIfAbsent(assetId, k -> new LinkedHashSet<>()).add(userId);
    }

    public void seedTags(String assetId, List<String> tags) {
        tagsByAsset.put(assetId, new LinkedHashSet<>(tags));
    }

    public Map<String, Asset> store() {
        return store;
    }

    // ---------- 端口实现 ----------

    @Override
    public Optional<Asset> findById(String id) {
        Asset asset = store.get(id);
        if (asset == null) {
            return Optional.empty();
        }
        if (asset.getSourceName() == null && asset.getSourceId() != null) {
            asset.setSourceName(sourceNames.get(asset.getSourceId()));
        }
        return Optional.of(asset);
    }

    @Override
    public Asset save(Asset asset) {
        store.put(asset.getId(), asset);
        return asset;
    }

    @Override
    public boolean isFavorite(String assetId, String userId) {
        Set<String> users = favoriteUsersByAsset.get(assetId);
        return users != null && users.contains(userId);
    }

    @Override
    public void addFavorite(String assetId, String userId) {
        favoriteUsersByAsset.computeIfAbsent(assetId, k -> new LinkedHashSet<>()).add(userId);
    }

    @Override
    public void removeFavorite(String assetId, String userId) {
        Set<String> users = favoriteUsersByAsset.get(assetId);
        if (users != null) {
            users.remove(userId);
        }
    }

    @Override
    public List<String> findTags(String assetId) {
        Set<String> tags = tagsByAsset.get(assetId);
        return tags == null ? Collections.emptyList() : new ArrayList<>(tags);
    }

    @Override
    public void replaceTags(String assetId, List<String> tags) {
        tagsByAsset.put(assetId, tags == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(tags));
    }

    @Override
    public List<AssetColumn> findColumns(String assetId) {
        List<AssetColumn> columns = columnsByAsset.get(assetId);
        return columns == null ? Collections.emptyList() : new ArrayList<>(columns);
    }

    @Override
    public void replaceColumns(String assetId, List<AssetColumn> columns) {
        columnsByAsset.put(assetId, columns == null ? new ArrayList<>() : new ArrayList<>(columns));
    }

    @Override
    public List<AssetVersion> findVersions(String assetId) {
        List<AssetVersion> versions = versionsByAsset.get(assetId);
        return versions == null ? Collections.emptyList() : new ArrayList<>(versions);
    }

    /** 供 InMemorySearchIndex 读取的按用户收藏集合（assetId → userIds）。 */
    public Map<String, Set<String>> favoriteUsersByAsset() {
        return favoriteUsersByAsset;
    }

    /** 供 InMemorySearchIndex 读取的字段清单。 */
    public Map<String, List<AssetColumn>> columnsByAsset() {
        return columnsByAsset;
    }

    /** 供 InMemorySearchIndex 读取的数据源名称映射。 */
    public Map<String, String> sourceNames() {
        return sourceNames;
    }

    /** 供 InMemorySearchIndex 读取的标签映射。 */
    public Map<String, Set<String>> tagsByAsset() {
        return tagsByAsset;
    }
}
