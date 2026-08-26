package com.yss.datamiddle.dqinsight.client.dto.query;

import com.yss.cloud.dto.page.PageQuery;
import com.yss.datamiddle.dqinsight.domain.model.AuditAction;
import lombok.Getter;
import lombok.Setter;

/**
 * 审计日志分页查询（GET /api/dq/audit-logs：page / size + action 筛选，只读不可变）。
 *
 * <p>action 为 null 时不过滤（返回全部 7 类动作）；0 条以空分页表达（冻结契约
 * AuditPage，非错误）。audit-logs 为管理员查询端点（操作权限守卫由切片 05 横切）。</p>
 */
@Getter
@Setter
public class AuditLogPageQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 动作筛选（7 类枚举：ingest / parse-fail / health-calc / channel-config /
     *  channel-toggle / channel-retry / linkage-map；null = 不过滤） */
    private AuditAction action;
}
