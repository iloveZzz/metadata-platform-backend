package com.yss.metadata.rest;

import com.yss.cloud.dto.result.SingleResult;
import com.yss.metadata.application.integration.service.DataHubExportService;
import com.yss.metadata.client.vo.ExportTaskVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * DataHub 导出控制器（冻结 OpenAPI /api/exports/datahub 段，WU-05-05）。
 *
 * <p>POST /api/exports/datahub：202 异步任务幂等（复用 export_task：asset_id NULL
 * 全局导出 + format=datahub）+ DataHubExporter SPI + 审计；目标未配置返回 422。</p>
 *
 * <p>当前用户上下文 seam（RBAC slice 06 替换）：operator 取请求头 X-User-Id
 * （缺省 default-user，见 {@link CurrentUser}）。</p>
 */
@RestController
@RequiredArgsConstructor
@Api(tags = "integrations")
public class DataHubExportController {

    private final DataHubExportService dataHubExportService;

    /**
     * 触发 DataHub 导出（202 ExportTask；进行中同目标任务幂等复用）。
     */
    @PostMapping("/api/exports/datahub")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ApiOperation(value = "触发 DataHub 导出", notes = "202 异步任务幂等复用；进行中任务重复触发返回既有任务；目标未配置返回 422")
    public SingleResult<ExportTaskVO> trigger(@RequestHeader(value = CurrentUser.HEADER, required = false) String userId) {
        return SingleResult.of(dataHubExportService.trigger(CurrentUser.resolve(userId)));
    }
}
