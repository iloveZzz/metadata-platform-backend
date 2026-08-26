package com.yss.datamiddle.dqinsight.domain.gateway;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.dqinsight.client.dto.query.AuditLogPageQuery;
import com.yss.datamiddle.dqinsight.client.vo.AuditLogVO;
import com.yss.datamiddle.dqinsight.domain.model.AuditLogEntry;

import java.util.List;

/**
 * 审计网关端口（dq_audit_log 只读不可变 append-only，独立写、不参与批次事务，数据架构 §7）。
 *
 * <p>读：审计日志分页查询（管理员端点，冻结契约 GET /api/dq/audit-logs；PageQuery 自动分页，
 * 总数经 query.tempTotalCount 回读；0 条以空分页表达）。</p>
 */
public interface AuditLogGateway {

    /**
     * 写一条审计记录（仅 INSERT）。
     */
    void record(AuditLogEntry entry);

    /**
     * 审计日志分页查询（action 筛选 + 时间倒序；PageQuery 自动分页；0 条以空分页表达）。
     */
    List<AuditLogVO> page(AuditLogPageQuery query);

    /**
     * 组装分页结果（PageQuery 自动分页的 total 在查询后回读）。
     */
    static PageResult<AuditLogVO> toPage(List<AuditLogVO> records, AuditLogPageQuery query) {
        return PageResult.of(records, query.getTempTotalCount(), query.getPageSize(), query.getPageIndex());
    }
}
