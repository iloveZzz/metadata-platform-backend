package com.yss.datamiddle.dqinsight.repository;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.datamiddle.dqinsight.repository.entity.DqRuleDetailPO;

/**
 * 规则明细快照仓储（UNIQUE(batch_id, asset_id, field_name, rule_name)；钻取按 (asset_id) 索引）。
 */
public interface DqRuleDetailRepository extends BasePlusRepository<DqRuleDetailPO> {
}
