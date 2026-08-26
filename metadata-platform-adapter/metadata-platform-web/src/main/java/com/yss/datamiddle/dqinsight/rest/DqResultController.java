package com.yss.datamiddle.dqinsight.rest;

import com.yss.cloud.dto.result.PageResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datamiddle.dqinsight.client.dto.query.IngestionRecordPageQuery;
import com.yss.datamiddle.dqinsight.client.vo.IngestionReceiptVO;
import com.yss.datamiddle.dqinsight.client.vo.IngestionRecordVO;
import com.yss.datamiddle.dqinsight.core.service.DqQueryAppService;
import com.yss.datamiddle.dqinsight.core.service.IngestionAppService;
import com.yss.datamiddle.dqinsight.domain.model.IngestionStatus;
import com.yss.datamiddle.dqinsight.domain.model.LinkageState;
import com.yss.datamiddle.dqinsight.domain.model.SourceTool;
import com.yss.datamiddle.dqinsight.rest.filter.ChannelAuthFilter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 外部 DQ 结果接入与接入记录查询（冻结 OpenAPI tag dq-results）。
 *
 * <p>POST 受通道级 Token 认证（B1，securitySchemes ChannelTokenAuth 已回写冻结 YAML）；
 * GET 走 DqQueryAppService 查询应用服务。</p>
 */
@RestController
@RequestMapping("/api/dq/results")
@RequiredArgsConstructor
@Api(tags = "dq-results")
public class DqResultController {

    private static final int MAX_PAGE_SIZE = 200;

    private final IngestionAppService ingestionAppService;
    private final DqQueryAppService dqQueryAppService;

    /**
     * 外部 DQ 结果接入（GE / 通用 CSV / 通用 API；幂等去重批次号）。
     */
    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, "text/csv"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation("外部 DQ 结果接入（GE / 通用 CSV / 通用 API；幂等去重批次号）")
    public SingleResult<IngestionReceiptVO> submit(
            @RequestBody String rawBody,
            @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String contentType,
            @RequestAttribute(value = ChannelAuthFilter.CHANNEL_ID_ATTRIBUTE, required = false) String channelId) {
        return SingleResult.of(ingestionAppService.ingest(rawBody, contentType, channelId));
    }

    /**
     * 接入记录查询（sourceTool / channelId / status / linkageStatus 筛选 + 分页，0 条以空分页表达）。
     */
    @GetMapping
    @ApiOperation("接入记录查询（含解析失败 / 错误分类 / 关联状态）")
    public PageResult<IngestionRecordVO> page(
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size,
            @RequestParam(value = "sourceTool", required = false) String sourceTool,
            @RequestParam(value = "channelId", required = false) String channelId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "linkageStatus", required = false) String linkageStatus) {
        IngestionRecordPageQuery query = new IngestionRecordPageQuery();
        query.setPageIndex(Math.max(page, 1));
        query.setPageSize(Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        query.setSourceTool(SourceTool.fromCodeOrNull(sourceTool));
        query.setChannelId(channelId);
        query.setStatus(IngestionStatus.fromCodeOrNull(status));
        query.setLinkageStatus(LinkageState.fromCodeOrNull(linkageStatus));
        return dqQueryAppService.pageIngestionRecords(query);
    }
}
