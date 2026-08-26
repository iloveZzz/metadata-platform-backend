package com.yss.metadata.repository;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.metadata.repository.entity.AssetFavoritePO;

/**
 * 用户收藏持久化仓库（MyBatis-Plus，BasePlusRepository 接入）。
 *
 * <p>复合主键 (asset_id, user_id)，读写均走 Wrapper（不依赖单列主键）。</p>
 */
public interface AssetFavoriteRepository extends BasePlusRepository<AssetFavoritePO> {
}
