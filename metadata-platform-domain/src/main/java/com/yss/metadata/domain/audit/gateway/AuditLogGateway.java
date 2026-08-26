package com.yss.metadata.domain.audit.gateway;

import com.yss.metadata.domain.audit.model.AuditLogEntry;
import com.yss.metadata.domain.audit.model.AuditLogPage;

/**
 * 审计日志端口（治理域；Domain 定义，Infrastructure 实现）。
 *
 * <p>audit_log 不可变：基础写入（人工补录/影响分析导出/集成配置/分类传播等
 * 各切片审计动作）+ slice 06 查询（分页 time DESC，只读）。</p>
 */
public interface AuditLogGateway {

    /**
     * 记录审计日志（追加式，不可变）。
     */
    void record(AuditLogEntry entry);

    /**
     * 分页查询审计日志（time DESC；只读不可变）。
     *
     * @param pageIndex 页码（从 1 起）
     * @param pageSize  每页大小
     * @return 分页结果（0 条以空分页表达，非错误）
     */
    AuditLogPage page(int pageIndex, int pageSize);
}
