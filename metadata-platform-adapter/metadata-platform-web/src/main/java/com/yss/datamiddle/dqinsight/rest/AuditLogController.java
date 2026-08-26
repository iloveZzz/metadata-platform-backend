package com.yss.datamiddle.dqinsight.rest;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.dqinsight.client.dto.query.AuditLogPageQuery;
import com.yss.datamiddle.dqinsight.client.vo.AuditLogVO;
import com.yss.datamiddle.dqinsight.core.service.DqQueryAppService;
import com.yss.datamiddle.dqinsight.domain.constant.DqCapabilities;
import com.yss.datamiddle.dqinsight.domain.model.AuditAction;
import com.yss.datamiddle.dqinsight.domain.service.DataDomainGuard;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审计日志查询（冻结 OpenAPI tag dq-audit；只读不可变，管理员端点）。
 *
 * <p>分页 + action 筛选（7 类枚举）；0 条以空分页表达；审计记录不可修改 / 删除
 * （append-only INSERT-only，C27）。操作权限守卫：无权限越权调用 403 err.dq.forbidden
 * （DQI-007，切片 05 横切）。</p>
 */
@RestController("dqAuditLogController")
@RequestMapping("/api/dq/audit-logs")
@RequiredArgsConstructor
@Api(tags = "dq-audit")
public class AuditLogController {

    private static final int MAX_PAGE_SIZE = 200;

    private final DqQueryAppService dqQueryAppService;
    private final DataDomainGuard dataDomainGuard;

    /**
     * 审计日志分页查询（action 筛选 + 时间倒序；0 条以空分页表达，非错误）。
     */
    @GetMapping
    @ApiOperation("审计日志查询（分页 + action 筛选；只读不可变）")
    public PageResult<AuditLogVO> page(
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size,
            @RequestParam(value = "action", required = false) String action) {
        dataDomainGuard.assertOperationAllowed(DqCapabilities.AUDIT_QUERY);
        AuditLogPageQuery query = new AuditLogPageQuery();
        query.setPageIndex(Math.max(page, 1));
        query.setPageSize(Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        query.setAction(AuditAction.fromCode(action));
        return dqQueryAppService.pageAuditLogs(query);
    }
}
