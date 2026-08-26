package com.yss.datamiddle.semantic.infrastructure.repository.mapper;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.datamiddle.semantic.infrastructure.repository.po.AuditLogPO;

/**
 * 审计日志 Mapper（audit_log 表，不可变只追加）。
 */
public interface AuditLogMapper extends BasePlusRepository<AuditLogPO> {
}
