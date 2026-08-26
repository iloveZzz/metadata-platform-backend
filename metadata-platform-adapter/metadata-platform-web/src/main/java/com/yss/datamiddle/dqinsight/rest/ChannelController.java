package com.yss.datamiddle.dqinsight.rest;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datamiddle.dqinsight.client.dto.ChannelCreateDTO;
import com.yss.datamiddle.dqinsight.client.dto.ChannelUpdateDTO;
import com.yss.datamiddle.dqinsight.client.vo.ChannelVO;
import com.yss.datamiddle.dqinsight.core.service.ChannelAppService;
import com.yss.datamiddle.dqinsight.domain.constant.DqCapabilities;
import com.yss.datamiddle.dqinsight.domain.service.DataDomainGuard;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 接入通道管理（冻结 OpenAPI tag dq-channels）。
 *
 * <p>CRUD + 启停（停用二次确认由前端交互承担）+ 重试拉取（幂等 409 busy）；认证凭证加密存储、
 * 密文不回传（仅 authConfigured，C19）；配置变更审计（SB-08）；操作类端点无权限越权调用
 * 403 err.dq.forbidden 兜底（DQI-007，切片 05 横切）。操作者当前用户上下文 MVP 以 X-Username 头
 * 解析，缺失回退 system（yss-userinfo starter 未入脚手架，人工审查点）。</p>
 */
@RestController
@RequestMapping("/api/dq/channels")
@RequiredArgsConstructor
@Api(tags = "dq-channels")
public class ChannelController {

    private final ChannelAppService channelAppService;
    private final DataDomainGuard dataDomainGuard;
    private final CurrentOperatorResolver currentOperatorResolver;

    /**
     * 通道列表（创建时间倒序；认证密文不回传）。
     */
    @GetMapping
    @ApiOperation("接入通道列表（启用 / 停用 / 拉取中 / 拉取失败）")
    public MultiResult<ChannelVO> list() {
        return MultiResult.of(channelAppService.listChannels());
    }

    /**
     * 新建通道（空态主操作；重名 409 err.dq.channel.name-conflict；无权限 403）。
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation("新建通道（配置变更留审计；认证配置加密存储）")
    public SingleResult<ChannelVO> create(@RequestBody ChannelCreateDTO dto) {
        dataDomainGuard.assertOperationAllowed(DqCapabilities.CHANNEL_CREATE);
        return SingleResult.of(channelAppService.createChannel(dto, currentOperatorResolver.currentOperator()));
    }

    /**
     * 通道详情。
     */
    @GetMapping("/{id}")
    @ApiOperation("通道详情")
    public SingleResult<ChannelVO> detail(@PathVariable Long id) {
        return SingleResult.of(channelAppService.getChannel(id));
    }

    /**
     * 更新通道配置 / 启停（部分更新至少一个字段 422；拉取中 409 busy；停用需二次确认；无权限 403）。
     */
    @PutMapping("/{id}")
    @ApiOperation("更新通道配置 / 启停（部分更新；配置变更留审计）")
    public SingleResult<ChannelVO> update(@PathVariable Long id, @RequestBody ChannelUpdateDTO dto) {
        dataDomainGuard.assertOperationAllowed(DqCapabilities.CHANNEL_UPDATE);
        return SingleResult.of(channelAppService.updateChannel(id, dto, currentOperatorResolver.currentOperator()));
    }

    /**
     * 删除通道（存在历史接入结果 409 err.dq.channel.in-use；不可逆；无权限 403）。
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation("删除通道（存在历史接入结果 409；不可逆）")
    public void delete(@PathVariable Long id) {
        dataDomainGuard.assertOperationAllowed(DqCapabilities.CHANNEL_DELETE);
        channelAppService.deleteChannel(id, currentOperatorResolver.currentOperator());
    }

    /**
     * 重试拉取（幂等：拉取中拒绝重复触发 409 err.dq.channel.busy；失败分类记录于通道错误字段；
     * 无权限 403）。
     */
    @PostMapping("/{id}/retry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ApiOperation("重试拉取（幂等；复用切片 01 接入管线）")
    public SingleResult<ChannelVO> retry(@PathVariable Long id) {
        dataDomainGuard.assertOperationAllowed(DqCapabilities.CHANNEL_RETRY);
        return SingleResult.of(channelAppService.retryPull(id, currentOperatorResolver.currentOperator()));
    }
}
