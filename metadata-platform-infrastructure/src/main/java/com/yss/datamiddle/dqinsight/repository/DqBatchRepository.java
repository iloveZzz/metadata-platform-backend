package com.yss.datamiddle.dqinsight.repository;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.datamiddle.dqinsight.repository.entity.DqBatchPO;

/**
 * 批次仓储（UNIQUE(source_tool, batch_no) 幂等去重兜底并发）。
 */
public interface DqBatchRepository extends BasePlusRepository<DqBatchPO> {
}
