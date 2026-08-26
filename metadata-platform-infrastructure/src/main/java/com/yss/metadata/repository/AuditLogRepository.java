package com.yss.metadata.repository;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.metadata.repository.entity.AuditLogPO;

/**
 * 审计日志持久化仓库（MyBatis-Plus，BasePlusRepository 接入；只追加，不可变）。
 */
public interface AuditLogRepository extends BasePlusRepository<AuditLogPO> {
}
