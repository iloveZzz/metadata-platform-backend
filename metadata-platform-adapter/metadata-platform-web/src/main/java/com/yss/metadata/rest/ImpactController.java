package com.yss.metadata.rest;

import com.yss.cloud.dto.result.SingleResult;
import com.yss.metadata.application.lineage.service.ImpactAnalysisService;
import com.yss.metadata.client.vo.ExportTaskVO;
import com.yss.metadata.client.vo.ImpactVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 影响分析控制器（冻结 OpenAPI impact 段，WU-03-05）。
 *
 * <p>GET /api/assets/{id}/impact-analysis 影响分析（sortBy depth/domain/risk）、
 * GET /api/assets/{id}/impact-analysis/export 导出（202 ExportTask；幂等复用；
 * audit_log 审计）。当前用户上下文 seam：导出 operator 取请求头 X-User-Id
 * （缺省 default-user，见 {@link CurrentUser}）。</p>
 */
@RestController
@RequiredArgsConstructor
@Api(tags = "impact")
public class ImpactController {

    private final ImpactAnalysisService impactAnalysisService;

    /**
     * 影响分析（下游全量召回，按影响深度分组）。
     */
    @GetMapping("/api/assets/{id}/impact-analysis")
    @ApiOperation(value = "影响分析", notes = "sortBy depth/domain/risk（默认 depth）；0 影响空结构非错误")
    public SingleResult<ImpactVO> impact(@PathVariable("id") String id,
                                         @RequestParam(name = "sortBy", defaultValue = "depth") String sortBy) {
        return SingleResult.of(impactAnalysisService.getImpact(id, sortBy));
    }

    /**
     * 导出影响分析（202 异步任务；幂等复用；审计）。
     */
    @GetMapping("/api/assets/{id}/impact-analysis/export")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ApiOperation(value = "导出影响分析", notes = "202 ExportTask；format csv/json（默认 csv）；同资产同格式进行中任务幂等复用；audit_log 审计")
    public SingleResult<ExportTaskVO> export(@PathVariable("id") String id,
                                             @RequestParam(name = "format", defaultValue = "csv") String format,
                                             @RequestHeader(value = CurrentUser.HEADER, required = false) String userId) {
        return SingleResult.of(impactAnalysisService.exportImpact(id, format, CurrentUser.resolve(userId)));
    }
}
