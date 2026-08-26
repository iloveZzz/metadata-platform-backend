package com.yss.metadata.rest;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.metadata.application.collector.service.CollectorInstanceAppService;
import com.yss.metadata.client.dto.cmd.BatchInstanceCmd;
import com.yss.metadata.client.dto.cmd.CollectorInstanceRerunCmd;
import com.yss.metadata.client.dto.cmd.CollectorInstanceTerminateCmd;
import com.yss.metadata.client.dto.query.CollectorInstanceQuery;
import com.yss.metadata.client.vo.CollectorInstanceVO;
import com.yss.metadata.client.vo.MetadataDiffSummaryVO;
import com.yss.metadata.client.vo.WorkflowNodeVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 采集实例控制器。
 */
@Validated
@RestController
@RequestMapping("/api/collector-instances")
@RequiredArgsConstructor
@Api(tags = "collector-instances")
public class CollectorInstanceController {

    private final CollectorInstanceAppService collectorInstanceAppService;

    /**
     * 采集实例列表查询（支持多维度过滤）。
     */
    @GetMapping
    @ApiOperation(value = "采集实例列表", notes = "多条件组合过滤查询采集实例列表")
    public MultiResult<CollectorInstanceVO> list(CollectorInstanceQuery query) {
        return MultiResult.of(collectorInstanceAppService.list(query));
    }

    /**
     * 采集实例详情查询。
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "采集实例详情", notes = "根据 ID 获取采集实例信息")
    public SingleResult<CollectorInstanceVO> getById(@PathVariable("id") String id) {
        return SingleResult.of(collectorInstanceAppService.getById(id));
    }

    /**
     * 获取采集变更概览比对。
     */
    @GetMapping("/{id}/diff-summary")
    @ApiOperation(value = "采集变更概览", notes = "获取本次采集对比上次成功采集的元数据变更概览")
    public SingleResult<MetadataDiffSummaryVO> getDiffSummary(@PathVariable("id") String id) {
        return SingleResult.of(collectorInstanceAppService.getDiffSummary(id));
    }

    /**
     * 重跑单个实例（仅失败实例支持）。
     */
    @PostMapping("/{id}/rerun")
    @ApiOperation(value = "重跑单个实例", notes = "重跑指定失败实例")
    public SingleResult<CollectorInstanceVO> rerun(@PathVariable("id") String id,
                                                  @Valid @RequestBody(required = false) CollectorInstanceRerunCmd cmd) {
        return SingleResult.of(collectorInstanceAppService.rerun(id, cmd));
    }

    /**
     * 批量重跑实例（仅失败实例被触发）。
     */
    @PostMapping("/batch-rerun")
    @ApiOperation(value = "批量重跑实例", notes = "批量重跑选中的失败实例")
    public MultiResult<CollectorInstanceVO> batchRerun(@Valid @RequestBody BatchInstanceCmd cmd) {
        return MultiResult.of(collectorInstanceAppService.batchRerun(cmd));
    }

    /**
     * 终止单个实例（仅运行中/等待中支持）。
     */
    @PostMapping("/{id}/terminate")
    @ApiOperation(value = "终止单个实例", notes = "终止指定运行中/等待中实例，终止后置为失败")
    public SingleResult<CollectorInstanceVO> terminate(@PathVariable("id") String id,
                                                      @Valid @RequestBody(required = false) CollectorInstanceTerminateCmd cmd) {
        return SingleResult.of(collectorInstanceAppService.terminate(id, cmd));
    }

    /**
     * 批量终止实例（仅运行中/等待中被触发）。
     */
    @PostMapping("/batch-terminate")
    @ApiOperation(value = "批量终止实例", notes = "批量终止选中的运行中/等待中实例")
    public MultiResult<CollectorInstanceVO> batchTerminate(@Valid @RequestBody BatchInstanceCmd cmd) {
        return MultiResult.of(collectorInstanceAppService.batchTerminate(cmd));
    }

    /**
     * 获取实例工作流节点日志与诊断列表。
     */
    @GetMapping("/{id}/nodes")
    @ApiOperation(value = "工作流节点日志与诊断", notes = "获取工作流节点树及节点执行日志、Dlink 4 大面板诊断信息")
    public MultiResult<WorkflowNodeVO> getWorkflowNodes(@PathVariable("id") String id) {
        return MultiResult.of(collectorInstanceAppService.getWorkflowNodes(id));
    }

    /**
     * 重跑单个工作流节点。
     */
    @PostMapping("/{id}/nodes/{nodeId}/rerun")
    @ApiOperation(value = "重跑单个工作流节点", notes = "重新执行工作流中指定的失败节点")
    public SingleResult<WorkflowNodeVO> rerunWorkflowNode(@PathVariable("id") String id,
                                                         @PathVariable("nodeId") String nodeId,
                                                         @RequestParam(value = "operator", required = false) String operator) {
        return SingleResult.of(collectorInstanceAppService.rerunWorkflowNode(id, nodeId, operator));
    }
}
