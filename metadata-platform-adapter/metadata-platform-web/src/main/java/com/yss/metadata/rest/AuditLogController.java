package com.yss.metadata.rest;

import com.yss.cloud.dto.result.PageResult;
import com.yss.metadata.application.rbac.service.AuditQueryService;
import com.yss.metadata.client.vo.AuditLogVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审计日志查询控制器（冻结 OpenAPI /api/audit-logs 段，WU-06-04）。
 *
 * <p>GET /api/audit-logs：分页 time DESC；只读不可变。管理端面：非管理员 403
 * （审计数据敏感，仅管理员可查）。</p>
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Api(tags = "rbac")
public class AuditLogController {

    private final AuditQueryService auditQueryService;

    /**
     * 审计日志分页查询（只读不可变）。
     */
    @GetMapping
    @ApiOperation(value = "审计日志查询", notes = "分页 time DESC；只读不可变；非管理员 403")
    public PageResult<AuditLogVO> page(@RequestParam(name = "page", defaultValue = "1") int page,
                                       @RequestParam(name = "size", defaultValue = "20") int size,
                                       @RequestHeader(value = RbacContext.ROLE_HEADER, required = false) String role) {
        RbacContext.requireAdmin(role);
        // 参数钳制：查询与回显一致（page ≥1；1 ≤ size ≤ 200，对齐冻结 API size maximum）
        int pageIndex = Math.max(1, page);
        int pageSize = Math.max(1, Math.min(size, 200));
        AuditQueryService.AuditPage result = auditQueryService.page(pageIndex, pageSize);
        return PageResult.of(result.getItems(), result.getTotal(), pageSize, pageIndex);
    }
}
