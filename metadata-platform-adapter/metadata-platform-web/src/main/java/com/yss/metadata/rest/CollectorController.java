package com.yss.metadata.rest;

import com.yss.metadata.application.collector.service.CollectorOrchestrator;
import com.yss.metadata.application.collector.service.CollectorTaskAppService;
import com.yss.metadata.client.dto.cmd.CollectorAddCmd;
import com.yss.metadata.client.dto.cmd.CollectorRetryCmd;
import com.yss.metadata.client.dto.cmd.CollectorRunCmd;
import com.yss.metadata.client.dto.cmd.CollectorUpdateCmd;
import com.yss.metadata.client.vo.CollectorVO;
import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.SingleResult;
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

import javax.validation.Valid;

/**
 * 采集任务控制器（冻结 OpenAPI collectors 段，WU-01-03）。
 *
 * <p>GET/POST /api/collectors、PUT /api/collectors/{id}、
 * POST /api/collectors/run、/api/collectors/{id}/cancel、/api/collectors/{id}/retry；
 * 响应统一 YSS Result 包装，错误体为 Error（code/message/severity/fieldErrors）。</p>
 *
 * <p>Web 层只做协议适配与响应包装，不做领域/VO 转换（由 Application 服务边界返回 VO）。</p>
 */
@RestController
@RequestMapping("/api/collectors")
@RequiredArgsConstructor
@Api(tags = "collectors")
public class CollectorController {

    private final CollectorTaskAppService collectorTaskAppService;
    private final CollectorOrchestrator collectorOrchestrator;

    /**
     * 采集任务列表（支持多条件过滤）。
     */
    @GetMapping
    @ApiOperation(value = "采集任务列表", notes = "返回 YSS Result 包装的采集任务列表，支持关键字、负责人、生效状态等多条件过滤")
    public MultiResult<CollectorVO> list(com.yss.metadata.client.dto.query.CollectorQuery query) {
        return MultiResult.of(collectorTaskAppService.list(query));
    }

    /**
     * 采集任务列表（全量无参调用兼容）。
     */
    public MultiResult<CollectorVO> list() {
        return list(null);
    }

    /**
     * 切换采集任务生效状态（启用/停用）。
     */
    @PutMapping("/{id}/status")
    @ApiOperation(value = "切换采集任务生效状态", notes = "启用/停用任务调度")
    public SingleResult<CollectorVO> toggleStatus(@PathVariable("id") String id,
                                                  @Valid @RequestBody com.yss.metadata.client.dto.cmd.CollectorStatusCmd cmd) {
        return SingleResult.of(collectorTaskAppService.toggleStatus(id, cmd.getEnabled()));
    }

    /**
     * 创建采集任务（同数据源 + 调度唯一，重复返回 409）。
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "创建采集任务", notes = "同数据源+调度唯一，重复返回 409；参数校验失败返回 422")
    public SingleResult<CollectorVO> create(@Valid @RequestBody CollectorAddCmd cmd) {
        return SingleResult.of(collectorTaskAppService.create(cmd));
    }

    /**
     * 获取采集任务详情（不存在返回 404）。
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "采集任务详情", notes = "根据 ID 获取采集任务详情，不存在返回 404")
    public SingleResult<CollectorVO> getById(@PathVariable("id") String id) {
        return SingleResult.of(collectorTaskAppService.getById(id));
    }

    /**
     * 编辑采集任务调度（配置变更后状态重置待执行）。
     */
    @PutMapping("/{id}")
    @ApiOperation(value = "编辑采集任务调度", notes = "采集任务不存在返回 404")
    public SingleResult<CollectorVO> update(@PathVariable("id") String id,
                                            @Valid @RequestBody CollectorUpdateCmd cmd) {
        return SingleResult.of(collectorTaskAppService.update(id, cmd));
    }

    /**
     * 删除采集任务（运行中删除返回 409，任务不存在返回 404）。
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "删除采集任务", notes = "删除指定采集任务及其调度排程，运行中删除返回 409 状态冲突，不存在返回 404")
    public void delete(@PathVariable("id") String id) {
        collectorTaskAppService.delete(id);
    }

    /**
     * 立即执行采集任务（幂等：运行中拒绝重复触发，409）。
     */
    @PostMapping("/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ApiOperation(value = "立即执行采集任务", notes = "运行中拒绝重复触发返回 409；执行结果写入任务状态")
    public SingleResult<CollectorVO> run(@Valid @RequestBody CollectorRunCmd cmd) {
        return SingleResult.of(collectorOrchestrator.run(cmd.getCollectorId()));
    }

    /**
     * 取消采集任务（仅运行中，409 状态冲突）。
     */
    @PostMapping("/{id}/cancel")
    @ApiOperation(value = "取消采集任务", notes = "仅运行中可取消；其余状态返回 409 状态冲突")
    public SingleResult<CollectorVO> cancel(@PathVariable("id") String id) {
        return SingleResult.of(collectorTaskAppService.cancel(id));
    }

    /**
     * 失败重试 / 局部重采（幂等；仅重采失败项，实际局部重采逻辑 seam-deferred）。
     */
    @PostMapping("/{id}/retry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ApiOperation(value = "失败重试/局部重采", notes = "运行中拒绝重复触发返回 409；仅重采失败项语义后置")
    public SingleResult<CollectorVO> retry(@PathVariable("id") String id,
                                           @Valid @RequestBody(required = false) CollectorRetryCmd cmd) {
        boolean failedItemsOnly = cmd == null || cmd.getFailedItemsOnly() == null
                || cmd.getFailedItemsOnly();
        return SingleResult.of(collectorOrchestrator.retry(id, failedItemsOnly));
    }
}
