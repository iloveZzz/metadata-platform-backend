package com.yss.datamiddle.dqinsight.repository;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.datamiddle.dqinsight.repository.entity.DqRuleResultPO;

/**
 * 规则结果仓储（单批次 ≤5 万行批量 INSERT）。
 */
public interface DqRuleResultRepository extends BasePlusRepository<DqRuleResultPO> {
}
