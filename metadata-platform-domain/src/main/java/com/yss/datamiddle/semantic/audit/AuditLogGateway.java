package com.yss.datamiddle.semantic.audit;

/**
 * 审计持久化网关（audit_log 不可变只追加 store）。
 *
 * <p>与业务写操作同事务（SL-SLICE-06 横切；本切片以本地 audit_log 为写审计 store）。
 * 跨平台聚合查询复用主平台 GET /api/audit-logs（seam-deferred，切片 06 补齐）。</p>
 */
public interface AuditLogGateway {

    /**
     * 追加审计记录（参与当前业务事务；业务失败回滚时一并回滚）。
     */
    void append(AuditLogEntry entry);

    /**
     * 追加被拒操作审计（REQUIRES_NEW 独立事务提交）。
     *
     * <p>只读用户直调写接口时记录 DENIED 审计后再抛 403（CT-10），
     * 必须独立于被拒绝的业务事务提交，避免被回滚吞掉。</p>
     */
    void appendDenied(AuditLogEntry entry);
}
