package com.yss.datamiddle.dqinsight.repository;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.datamiddle.dqinsight.repository.entity.DqAssetLinkagePO;

/**
 * 资产关联仓储（pending 队列 = state = pending）。
 */
public interface DqAssetLinkageRepository extends BasePlusRepository<DqAssetLinkagePO> {
}
