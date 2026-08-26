package com.yss.datamiddle.dqinsight.repository;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.datamiddle.dqinsight.repository.entity.DqAuditLogPO;

/**
 * 审计仓储（只读不可变 append-only，仅 INSERT）。
 */
public interface DqAuditLogRepository extends BasePlusRepository<DqAuditLogPO> {
}
