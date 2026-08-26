package com.yss.metadata.repository;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.metadata.repository.entity.OpenLineageEventPO;

/**
 * OpenLineage 事件记录持久化仓库（MyBatis-Plus，BasePlusRepository 接入）。
 */
public interface OpenLineageEventRepository extends BasePlusRepository<OpenLineageEventPO> {
}
