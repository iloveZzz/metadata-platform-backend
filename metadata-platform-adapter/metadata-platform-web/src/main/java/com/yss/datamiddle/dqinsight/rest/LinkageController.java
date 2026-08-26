package com.yss.datamiddle.dqinsight.rest;

import com.yss.cloud.dto.result.PageResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datamiddle.dqinsight.client.dto.LinkageMapDTO;
import com.yss.datamiddle.dqinsight.client.dto.query.PendingLinkagePageQuery;
import com.yss.datamiddle.dqinsight.client.vo.LinkageResultVO;
import com.yss.datamiddle.dqinsight.client.vo.PendingLinkageVO;
import com.yss.datamiddle.dqinsight.core.service.LinkageAppService;
import com.yss.datamiddle.dqinsight.domain.constant.DqCapabilities;
import com.yss.datamiddle.dqinsight.domain.service.DataDomainGuard;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 资产关联治理（冻结 OpenAPI tag dq-linkage）。
 *
 * <p>待关联队列（未命中，结果已入库，空队列以空分页表达；切片 05 数据域过滤：
 * 待关联资产归属 = 来源通道域，域外不展示 C24）；人工映射（防腐层校验 → 覆盖确认 →
 * 触发健康分首次计算 → 审计 linkage-map，C26；操作权限 403 兜底 DQI-007）。
 * 操作者当前用户上下文 MVP 以 X-Username 头解析，缺失回退 system（人工审查点）。</p>
 */
@RestController
@RequestMapping("/api/dq/asset-linkage")
@RequiredArgsConstructor
@Api(tags = "dq-linkage")
public class LinkageController {

    private static final int MAX_PAGE_SIZE = 200;

    private final LinkageAppService linkageAppService;
    private final DataDomainGuard dataDomainGuard;
    private final CurrentOperatorResolver currentOperatorResolver;

    /**
     * 待关联资产队列（资产 ID 未命中的人工映射入口；分页；空队列以空分页表达；域外不展示）。
     */
    @GetMapping("/pending")
    @ApiOperation("待关联资产队列（未命中，结果已入库；分页；数据域过滤）")
    public PageResult<PendingLinkageVO> pending(
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {
        PendingLinkagePageQuery query = new PendingLinkagePageQuery();
        query.setPageIndex(Math.max(page, 1));
        query.setPageSize(Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        List<String> visible = dataDomainGuard.visibleDomains();
        query.setVisibleDomains(visible);
        return linkageAppService.listPending(query);
    }

    /**
     * 人工映射（目标资产不存在 422 err.dq.asset.not-found；已关联 409 err.dq.linkage.already-linked +
     * confirmOverwrite 二次确认；映射后触发健康分首次计算，复用切片 02 计算入口；
     * 无操作权限越权调用 403 err.dq.forbidden）。
     */
    @PostMapping("/{id}/map")
    @ApiOperation("人工映射（保存资产快照并触发健康分首次计算；覆盖需二次确认）")
    public SingleResult<LinkageResultVO> map(@PathVariable Long id, @RequestBody LinkageMapDTO dto) {
        dataDomainGuard.assertOperationAllowed(DqCapabilities.LINKAGE_MAP);
        return SingleResult.of(linkageAppService.mapLinkage(id, dto, currentOperatorResolver.currentOperator()));
    }
}
