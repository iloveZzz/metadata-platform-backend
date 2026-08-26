package com.yss.metadata.repository;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.metadata.repository.entity.AssetTagPO;

/**
 * 资产标签持久化仓库（MyBatis-Plus，BasePlusRepository 接入）。
 *
 * <p>复合主键 (asset_id, tag)，读写均走 Wrapper（不依赖单列主键）。</p>
 */
public interface AssetTagRepository extends BasePlusRepository<AssetTagPO> {
}
