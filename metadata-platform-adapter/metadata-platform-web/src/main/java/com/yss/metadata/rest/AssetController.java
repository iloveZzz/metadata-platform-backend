package com.yss.metadata.rest;

import com.yss.cloud.dto.result.PageResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.metadata.application.asset.service.AssetActionService;
import com.yss.metadata.application.asset.service.AssetQueryService;
import com.yss.metadata.application.dq.TaintStatusApplicationService;
import com.yss.metadata.client.dto.cmd.AssetTagUpdateCmd;
import com.yss.metadata.client.dto.cmd.TaintStatusCmd;
import com.yss.metadata.client.dto.query.AssetSearchQuery;
import com.yss.metadata.client.vo.AssetDetailVO;
import com.yss.metadata.client.vo.AssetPageVO;
import com.yss.metadata.client.vo.AssetVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 资产目录控制器（冻结 OpenAPI assets 段，WU-02-01 ~ WU-02-04）。
 *
 * <p>GET /api/assets 搜索（列级命中/筛选/排序/分页，PageResult 包装）、
 * GET /api/assets/{id} 详情聚合、POST favorite/claim/archive/unarchive、
 * PUT /api/assets/{id}/tags、PUT /api/assets/{id}/taint-status；错误体为 Error（code/message/severity/fieldErrors）。</p>
 *
 * <p>当前用户上下文 seam（RBAC slice 06 替换）：收藏/认领/我的资产从请求头
 * X-User-Id 解析（缺省 default-user，见 {@link CurrentUser}）。</p>
 */
@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@Api(tags = "assets")
public class AssetController {

    private final AssetQueryService assetQueryService;
    private final AssetActionService assetActionService;
    private final TaintStatusApplicationService taintStatusApplicationService;


    /**
     * 资产列表 / 搜索（分页/过滤/排序/数据域过滤/收藏/我的资产；列级命中）。
     */
    @GetMapping
    @ApiOperation(value = "资产列表/搜索", notes = "PageResult 包装；0 条以空分页表达（非错误）；默认按更新时间倒序")
    public PageResult<AssetVO> search(AssetSearchQuery query,
                                      @RequestHeader(value = CurrentUser.HEADER, required = false) String userId,
                                      @RequestHeader(value = RbacContext.DOMAINS_HEADER, required = false) String domainsHeader) {
        query.setCurrentUserId(CurrentUser.resolve(userId));
        // slice 06 RBAC：数据域过滤（X-User-Domains 头；缺省全部放行）
        query.setAllowedDomains(RbacContext.resolveDomains(domainsHeader));
        AssetPageVO page = assetQueryService.search(query);
        return PageResult.of(page.getList(), page.getTotal(), page.getSize(), page.getPage());
    }

    /**
     * 资产详情聚合（元数据 + 字段清单 + 版本/变更记录 + 标签 + 收藏状态）。
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "资产详情聚合", notes = "资产不存在返回 404")
    public SingleResult<AssetDetailVO> detail(@PathVariable("id") String id,
                                              @RequestHeader(value = CurrentUser.HEADER, required = false) String userId) {
        return SingleResult.of(assetQueryService.getDetail(id, CurrentUser.resolve(userId)));
    }

    /**
     * 收藏 / 取消收藏（幂等切换）。
     */
    @PostMapping("/{id}/favorite")
    @ApiOperation(value = "收藏/取消收藏", notes = "幂等切换，返回最新收藏状态")
    public SingleResult<AssetVO> favorite(@PathVariable("id") String id,
                                          @RequestHeader(value = CurrentUser.HEADER, required = false) String userId) {
        return SingleResult.of(assetActionService.toggleFavorite(id, CurrentUser.resolve(userId)));
    }

    /**
     * 认领 owner（已认领他人返回 409）。
     */
    @PostMapping("/{id}/claim")
    @ApiOperation(value = "认领 owner", notes = "已被他人认领返回 409；本人重复认领幂等")
    public SingleResult<AssetVO> claim(@PathVariable("id") String id,
                                       @RequestHeader(value = CurrentUser.HEADER, required = false) String userId) {
        return SingleResult.of(assetActionService.claim(id, CurrentUser.resolve(userId)));
    }

    /**
     * 编辑标签（覆盖式更新）。
     */
    @PutMapping("/{id}/tags")
    @ApiOperation(value = "编辑标签", notes = "覆盖式全量替换；归档/已删除资产返回 409 状态冲突")
    public SingleResult<AssetVO> updateTags(@PathVariable("id") String id,
                                            @Valid @RequestBody AssetTagUpdateCmd cmd) {
        return SingleResult.of(assetActionService.updateTags(id, cmd.getTags()));
    }

    /**
     * 归档资产（治理专员；归档后只读；重复归档返回 409）。
     */
    @PostMapping("/{id}/archive")
    @ApiOperation(value = "归档资产", notes = "重复归档/已删除资产返回 409 状态冲突")
    public SingleResult<AssetVO> archive(@PathVariable("id") String id) {
        return SingleResult.of(assetActionService.archive(id));
    }

    /**
     * 取消归档（恢复可编辑）。
     */
    @PostMapping("/{id}/unarchive")
    @ApiOperation(value = "取消归档", notes = "已归档恢复可编辑；非归档状态幂等")
    public SingleResult<AssetVO> unarchive(@PathVariable("id") String id) {
        return SingleResult.of(assetActionService.unarchive(id));
    }

    /**
     * 标记/解除全链路数据存疑状态 (PUT /api/assets/{id}/taint-status)
     */
    @PutMapping("/{id}/taint-status")
    @ApiOperation(value = "标记/解除全链路数据存疑状态", notes = "更新存疑状态 (NORMAL/TAINTED) 并写入审计日志")
    public SingleResult<AssetVO> updateTaintStatus(@PathVariable("id") String id,
                                                   @Valid @RequestBody TaintStatusCmd cmd,
                                                   @RequestHeader(value = CurrentUser.HEADER, required = false) String userId) {
        taintStatusApplicationService.updateTaintStatus(id, cmd.getTaintStatus(), cmd.getReason(), CurrentUser.resolve(userId));
        return SingleResult.of(assetQueryService.getDetail(id, CurrentUser.resolve(userId)));
    }

    /**
     * 剔除/软删除资产 (PUT /api/assets/{id}/exclude)。
     */
    @PutMapping("/{id}/exclude")
    @ApiOperation(value = "剔除资产", notes = "将资产标记为已剔除（软删除），默认列表中隐藏；已归档只读资产返回 409")
    public SingleResult<AssetVO> exclude(@PathVariable("id") String id) {
        return SingleResult.of(assetActionService.exclude(id));
    }

    /**
     * 恢复已剔除资产 (PUT /api/assets/{id}/recover)。
     */
    @PutMapping("/{id}/recover")
    @ApiOperation(value = "恢复资产", notes = "将已剔除资产重新恢复至正常清单")
    public SingleResult<AssetVO> recover(@PathVariable("id") String id) {
        return SingleResult.of(assetActionService.recover(id));
    }
}
