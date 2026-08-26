package com.yss.datamiddle.dqinsight.repository;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.datamiddle.dqinsight.repository.entity.DqHealthScorePO;

/**
 * 健康分仓储（UNIQUE(asset_id, field_name) 每资产 / 字段保留最新；C28 聚合索引覆盖）。
 */
public interface DqHealthScoreRepository extends BasePlusRepository<DqHealthScorePO> {
}
