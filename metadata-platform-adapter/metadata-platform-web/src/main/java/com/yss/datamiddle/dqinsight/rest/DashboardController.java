package com.yss.datamiddle.dqinsight.rest;

import com.yss.cloud.dto.result.SingleResult;
import com.yss.datamiddle.dqinsight.client.dto.query.HealthScorePageQuery;
import com.yss.datamiddle.dqinsight.client.vo.DashboardVO;
import com.yss.datamiddle.dqinsight.core.service.DqQueryAppService;
import com.yss.datamiddle.dqinsight.domain.model.BandFilter;
import com.yss.datamiddle.dqinsight.domain.model.DashboardSort;
import com.yss.datamiddle.dqinsight.domain.service.DataDomainGuard;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 健康分仪表盘聚合（冻结 OpenAPI tag dq-dashboard）。
 *
 * <p>聚合统计基于数据域内可见资产全集（DataDomainGuard 可见域横切 + domain / assetType 筛选，
 * 不含 band）；资产列表支持分页 / 筛选 / 排序（score / lastResultAt / name，默认 score）；
 * 0 条以空分页表达，非错误。覆盖率口径 SB-07（targetAssetCount 来自防腐层）。
 * 域过滤 RBAC 实现切片 05 落地（RbacDataDomainFilter，C24 域外不展示）。</p>
 */
@RestController
@RequestMapping("/api/dq/dashboard")
@RequiredArgsConstructor
@Api(tags = "dq-dashboard")
public class DashboardController {

    private static final int MAX_PAGE_SIZE = 200;

    private final DqQueryAppService dqQueryAppService;
    private final DataDomainGuard dataDomainGuard;

    /**
     * 仪表盘聚合（stats + 资产健康分列表分页筛选排序；bandDistribution 含过期 / 无结果独立展示态）。
     */
    @GetMapping
    @ApiOperation("仪表盘聚合（健康分分布 / 已接入 / 低分 / 覆盖率 + 资产列表分页筛选排序）")
    public SingleResult<DashboardVO> dashboard(
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size,
            @RequestParam(value = "domain", required = false) String domain,
            @RequestParam(value = "band", required = false) String band,
            @RequestParam(value = "assetType", required = false) String assetType,
            @RequestParam(value = "sort", required = false, defaultValue = "score") String sort) {
        HealthScorePageQuery query = new HealthScorePageQuery();
        query.setPageIndex(Math.max(page, 1));
        query.setPageSize(Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        query.setDomain(domain);
        query.setBand(BandFilter.fromCodeOrNull(band));
        query.setAssetType(assetType);
        DashboardSort parsedSort = DashboardSort.fromCodeOrNull(sort);
        query.setSort(parsedSort == null ? DashboardSort.SCORE : parsedSort);
        List<String> visible = dataDomainGuard.visibleDomains();
        query.setVisibleDomains(visible);

        return SingleResult.of(dqQueryAppService.getDashboard(query));
    }
}
