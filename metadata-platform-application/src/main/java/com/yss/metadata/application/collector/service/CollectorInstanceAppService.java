package com.yss.metadata.application.collector.service;

import com.yss.metadata.client.dto.cmd.BatchInstanceCmd;
import com.yss.metadata.client.dto.cmd.CollectorInstanceRerunCmd;
import com.yss.metadata.client.dto.cmd.CollectorInstanceTerminateCmd;
import com.yss.metadata.client.dto.query.CollectorInstanceQuery;
import com.yss.metadata.client.vo.CollectorInstanceVO;
import com.yss.metadata.client.vo.MetadataDiffSummaryVO;
import com.yss.metadata.client.vo.WorkflowNodeVO;

import java.util.List;

/**
 * 采集实例应用服务接口。
 */
public interface CollectorInstanceAppService {

    /**
     * 条件查询采集实例列表。
     */
    List<CollectorInstanceVO> list(CollectorInstanceQuery query);

    /**
     * 根据 ID 获取采集实例详情。
     */
    CollectorInstanceVO getById(String id);

    /**
     * 获取采集实例变更概览比对。
     */
    MetadataDiffSummaryVO getDiffSummary(String id);

    /**
     * 重跑单实例（仅失败实例支持）。
     */
    CollectorInstanceVO rerun(String id, CollectorInstanceRerunCmd cmd);

    /**
     * 批量重跑（仅失败实例被触发）。
     */
    List<CollectorInstanceVO> batchRerun(BatchInstanceCmd cmd);

    /**
     * 终止单实例（仅运行中/等待中支持）。
     */
    CollectorInstanceVO terminate(String id, CollectorInstanceTerminateCmd cmd);

    /**
     * 批量终止（仅运行中/等待中被触发）。
     */
    List<CollectorInstanceVO> batchTerminate(BatchInstanceCmd cmd);

    /**
     * 获取实例工作流节点日志与诊断列表。
     */
    List<WorkflowNodeVO> getWorkflowNodes(String instanceId);

    /**
     * 重跑单个工作流节点。
     */
    WorkflowNodeVO rerunWorkflowNode(String instanceId, String nodeId, String operator);
}
