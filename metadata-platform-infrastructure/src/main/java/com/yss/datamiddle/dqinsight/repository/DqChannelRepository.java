package com.yss.datamiddle.dqinsight.repository;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.datamiddle.dqinsight.repository.entity.DqChannelPO;

/**
 * 通道仓储（name 未删除唯一约束兜底并发；updated_at 乐观并发版本位）。
 */
public interface DqChannelRepository extends BasePlusRepository<DqChannelPO> {
}
