package com.yss.datamiddle.dqinsight.rest;

import com.yss.cloud.dto.result.PageResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datamiddle.dqinsight.client.dto.query.HealthScorePageQuery;
import com.yss.datamiddle.dqinsight.client.vo.AssetHealthDetailVO;
import com.yss.datamiddle.dqinsight.client.vo.AssetHealthRowVO;
import com.yss.datamiddle.dqinsight.client.vo.RuleDetailVO;
import com.yss.datamiddle.dqinsight.core.service.DqQueryAppService;
import com.yss.datamiddle.dqinsight.domain.exception.HealthScoreNotFoundException;
import com.yss.datamiddle.dqinsight.domain.model.BandFilter;
import com.yss.datamiddle.dqinsight.domain.service.DataDomainGuard;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 健康分查询与规则明细钻取（冻结 OpenAPI tag dq-health）。
 *
 * <p>查询走 DqQueryAppService 应用服务；YSS 统一响应包装 SingleResult / PageResult；
 * 404 语义 err.dq.not-found（健康分 / 明细无对应资产）；过期 / 无结果独立展示态随行返回（OQ-03 / SB-03）。
 * 数据域 RBAC 切片 05 落地（C24）：列表域外不展示（visibleDomains 横切），直连域外详情 /
 * 钻取 403 err.dq.forbidden（错误信息不含资源标识，不泄露存在性）。</p>
 */
@RestController
@RequestMapping("/api/dq/health")
@RequiredArgsConstructor
@Api(tags = "dq-health")
public class HealthScoreController {

    private static final int MAX_PAGE_SIZE = 200;

    private final DqQueryAppService dqQueryAppService;
    private final DataDomainGuard dataDomainGuard;

    /**
     * 资产级健康分列表（分页 / 筛选；assetId 精确跳转；档位 + 独立展示态字段随行返回；0 条空分页）。
     */
    @GetMapping
    @ApiOperation("资产级健康分列表（档位 + 独立展示态 expired / noresult 字段，分页 / 筛选）")
    public PageResult<AssetHealthRowVO> page(
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size,
            @RequestParam(value = "assetId", required = false) String assetId,
            @RequestParam(value = "domain", required = false) String domain,
            @RequestParam(value = "band", required = false) String band,
            @RequestParam(value = "assetType", required = false) String assetType) {
        HealthScorePageQuery query = new HealthScorePageQuery();
        query.setPageIndex(Math.max(page, 1));
        query.setPageSize(Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        query.setAssetId(assetId);
        query.setDomain(domain);
        query.setBand(BandFilter.fromCodeOrNull(band));
        query.setAssetType(assetType);
        List<String> visible = dataDomainGuard.visibleDomains();
        query.setVisibleDomains(visible);
        return dqQueryAppService.pageAssetHealth(query);
    }

    /**
     * 资产级 + 字段级健康分详情（lowScore 低分字段 / ruleVersion / sourceTool；无健康分 404；
     * 域外资产直连 403 err.dq.forbidden，不泄露资源存在性）。
     */
    @GetMapping("/{assetId}")
    @ApiOperation("资产级 + 字段级健康分详情（低分字段 / 计算规则版本 / 来源工具）")
    public SingleResult<AssetHealthDetailVO> detail(@PathVariable String assetId) {
        AssetHealthDetailVO vo = dqQueryAppService.getAssetHealthDetail(assetId);
        if (vo == null) {
            throw new HealthScoreNotFoundException(assetId);
        }
        dataDomainGuard.assertViewAllowed(vo.getDomain());
        return SingleResult.of(vo);
    }

    /**
     * 规则明细钻取（分数来源区公式 + 权重 + 算法说明 / ruleVersion / expired 标识；fieldName 过滤；
     * 域外资产直连 403，与详情一致不泄露存在性）。
     */
    @GetMapping("/{assetId}/details")
    @ApiOperation("规则明细钻取（分数来源区公式与权重 / 规则明细 / 过期标识）")
    public SingleResult<RuleDetailVO> details(
            @PathVariable String assetId,
            @RequestParam(value = "fieldName", required = false) String fieldName) {
        AssetHealthDetailVO detail = dqQueryAppService.getAssetHealthDetail(assetId);
        if (detail == null) {
            throw new HealthScoreNotFoundException(assetId);
        }
        dataDomainGuard.assertViewAllowed(detail.getDomain());
        RuleDetailVO vo = dqQueryAppService.getRuleDetail(assetId, fieldName);
        if (vo == null) {
            throw new HealthScoreNotFoundException(assetId);
        }
        return SingleResult.of(vo);
    }
}
