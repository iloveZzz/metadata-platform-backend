package com.yss.metadata.application.rbac.service;

import com.yss.metadata.client.vo.AuditLogVO;

import java.util.List;

/**
 * 审计查询应用服务（FR-018；WU-06-02）。
 *
 * <p>GET /api/audit-logs：分页 time DESC；只读不可变。</p>
 */
public interface AuditQueryService {

    /**
     * 分页查询审计日志（time DESC）。
     *
     * @return 分页结果（items + total）
     */
    AuditPage page(int pageIndex, int pageSize);

    /** 审计分页结果（items + total） */
    class AuditPage {
        private final List<AuditLogVO> items;
        private final long total;

        public AuditPage(List<AuditLogVO> items, long total) {
            this.items = items;
            this.total = total;
        }

        public List<AuditLogVO> getItems() {
            return items;
        }

        public long getTotal() {
            return total;
        }
    }
}
