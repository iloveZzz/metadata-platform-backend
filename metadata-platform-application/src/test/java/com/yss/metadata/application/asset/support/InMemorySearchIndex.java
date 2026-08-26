package com.yss.metadata.application.asset.support;

import com.yss.metadata.client.dto.query.AssetSearchQuery;
import com.yss.metadata.domain.asset.gateway.SearchIndex;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetColumn;
import com.yss.metadata.domain.asset.model.AssetSearchResult;
import com.yss.metadata.domain.asset.model.AssetSort;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 资产发现索引内存实现（测试 seam；与 InMemoryAssetRepository 共享存储）。
 *
 * <p>按 SearchIndex 契约实现列级命中/筛选/排序/分页的参考语义，
 * 供应用层编排测试与 Web 契约测试使用。</p>
 */
public class InMemorySearchIndex implements SearchIndex {

    private final InMemoryAssetRepository repository;

    public InMemorySearchIndex(InMemoryAssetRepository repository) {
        this.repository = repository;
    }

    @Override
    public AssetSearchResult search(AssetSearchQuery query) {
        List<Asset> matched = repository.store().values().stream()
                .filter(asset -> Boolean.TRUE.equals(query.getIsExcluded()) ? Boolean.TRUE.equals(asset.getIsExcluded()) : !Boolean.TRUE.equals(asset.getIsExcluded()))
                .filter(asset -> matchKeyword(asset, query.getKeyword()))
                .filter(asset -> matchSource(asset, query.getSource()))
                .filter(asset -> !StringUtils.hasText(query.getSourceId()) || query.getSourceId().equals(asset.getSourceId()))
                .filter(asset -> !StringUtils.hasText(query.getDatabase()) || query.getDatabase().equals(asset.getDatabaseName()))
                .filter(asset -> !StringUtils.hasText(query.getSourceSystem()) || query.getSourceSystem().equals(asset.getSourceSystem()))
                .filter(asset -> matchEq(asset.getType(), query.getType()))
                .filter(asset -> matchEq(asset.getDomain(), query.getDomain()))
                .filter(asset -> matchEq(asset.getClassification(), query.getClassification()))
                .filter(asset -> matchAllowedDomains(asset, query.getAllowedDomains()))
                .filter(asset -> matchFavorite(asset, query.getFavorite(), query.getCurrentUserId()))
                .filter(asset -> matchMine(asset, query.getMine(), query.getCurrentUserId()))
                .sorted(sortComparator(AssetSort.fromValue(query.getSort())))
                .collect(Collectors.toList());

        long total = matched.size();
        int pageIndex = Math.max(query.getPageIndex(), 1);
        int pageSize = Math.max(query.getPageSize(), 1);
        int from = Math.min((pageIndex - 1) * pageSize, matched.size());
        int to = Math.min(from + pageSize, matched.size());
        List<Asset> items = new ArrayList<>(matched.subList(from, to));
        // 组合字段：收藏状态与数据源名称
        for (Asset asset : items) {
            asset.setFavorite(repository.isFavorite(asset.getId(), query.getCurrentUserId()));
            asset.setSourceName(repository.sourceNames().get(asset.getSourceId()));
        }
        return AssetSearchResult.builder()
                .items(items)
                .total(total)
                .pageIndex(pageIndex)
                .pageSize(pageSize)
                .build();
    }

    private boolean matchKeyword(Asset asset, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String kw = keyword.trim().toLowerCase();
        if (asset.getName() != null && asset.getName().toLowerCase().contains(kw)) {
            return true;
        }
        // 列级命中：keyword 命中字段名称时返回资产行
        List<AssetColumn> columns = repository.columnsByAsset().get(asset.getId());
        if (columns != null) {
            for (AssetColumn column : columns) {
                if (column.getName() != null && column.getName().toLowerCase().contains(kw)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchSource(Asset asset, String source) {
        if (!StringUtils.hasText(source)) {
            return true;
        }
        String name = repository.sourceNames().get(asset.getSourceId());
        return source.equals(name);
    }

    private boolean matchEq(String actual, String expected) {
        return !StringUtils.hasText(expected) || expected.equals(actual);
    }

    /** slice 06 RBAC：允许数据域过滤（null/空 = 全部放行） */
    private boolean matchAllowedDomains(Asset asset, List<String> allowedDomains) {
        if (allowedDomains == null || allowedDomains.isEmpty()) {
            return true;
        }
        return asset.getDomain() != null && allowedDomains.contains(asset.getDomain());
    }

    private boolean matchFavorite(Asset asset, Boolean favorite, String currentUserId) {
        if (!Boolean.TRUE.equals(favorite)) {
            return true;
        }
        Set<String> users = repository.favoriteUsersByAsset().get(asset.getId());
        return users != null && currentUserId != null && users.contains(currentUserId);
    }

    private boolean matchMine(Asset asset, Boolean mine, String currentUserId) {
        if (!Boolean.TRUE.equals(mine)) {
            return true;
        }
        return currentUserId != null && currentUserId.equals(asset.getOwner());
    }

    private Comparator<Asset> sortComparator(AssetSort sort) {
        switch (sort) {
            case NAME:
                return Comparator.comparing(Asset::getName, Comparator.nullsLast(String::compareTo));
            case CLASSIFICATION:
                return Comparator.comparing(Asset::getClassification, Comparator.nullsLast(String::compareTo));
            default:
                return Comparator.comparing(Asset::getUpdatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        }
    }
}
