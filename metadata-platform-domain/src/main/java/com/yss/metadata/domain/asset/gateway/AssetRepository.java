package com.yss.metadata.domain.asset.gateway;

import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetColumn;
import com.yss.metadata.domain.asset.model.AssetVersion;

import java.util.List;
import java.util.Optional;

/**
 * 资产仓储端口（目录上下文；Domain 定义，Infrastructure 实现）。
 *
 * <p>覆盖资产读写、收藏（按用户多值，幂等切换）与标签（覆盖式更新）；
 * 搜索走发现上下文 {@link SearchIndex} 端口（可替换 seam）。</p>
 *
 * <p>收藏/认领的当前用户上下文 seam：RBAC（slice 06）前由应用服务传入
 * 请求头 X-User-Id 解析值（缺省 default-user），slice 06 替换。</p>
 */
public interface AssetRepository {

    /**
     * 按 id 查询资产（含数据源名称组合字段）。
     */
    Optional<Asset> findById(String id);

    /**
     * 保存资产（新增或更新）。
     */
    Asset save(Asset asset);

    /**
     * 当前用户是否已收藏该资产。
     */
    boolean isFavorite(String assetId, String userId);

    /**
     * 添加收藏（幂等：已收藏则无操作）。
     */
    void addFavorite(String assetId, String userId);

    /**
     * 取消收藏（幂等：未收藏则无操作）。
     */
    void removeFavorite(String assetId, String userId);

    /**
     * 查询资产标签（升序）。
     */
    List<String> findTags(String assetId);

    /**
     * 覆盖式更新标签（全量替换）。
     */
    void replaceTags(String assetId, List<String> tags);

    /**
     * 查询资产字段清单（升序）。
     */
    List<AssetColumn> findColumns(String assetId);

    /**
     * 覆盖式更新字段清单（全量替换）。
     */
    void replaceColumns(String assetId, List<AssetColumn> columns);

    /**
     * 查询资产版本/变更记录（版本倒序，最新在前）。
     */
    List<AssetVersion> findVersions(String assetId);
}
