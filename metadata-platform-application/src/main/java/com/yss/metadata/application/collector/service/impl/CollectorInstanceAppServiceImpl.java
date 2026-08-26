package com.yss.metadata.application.collector.service.impl;

import com.yss.metadata.application.collector.service.CollectorInstanceAppService;
import com.yss.metadata.application.collector.service.convertor.CollectorInstanceAppConvertor;
import com.yss.metadata.client.dto.cmd.BatchInstanceCmd;
import com.yss.metadata.client.dto.cmd.CollectorInstanceRerunCmd;
import com.yss.metadata.client.dto.cmd.CollectorInstanceTerminateCmd;
import com.yss.metadata.client.dto.query.CollectorInstanceQuery;
import com.yss.metadata.client.vo.CollectorInstanceVO;
import com.yss.metadata.client.vo.MetadataDiffSummaryVO;
import com.yss.metadata.client.vo.WorkflowNodeVO;
import com.yss.metadata.domain.collector.exception.CollectorInstanceNotFoundException;
import com.yss.metadata.domain.collector.gateway.CollectorInstanceGateway;
import com.yss.metadata.domain.collector.model.CollectorInstance;
import com.yss.metadata.domain.collector.model.CollectorInstanceStatus;
import com.yss.metadata.domain.collector.model.ExecutionMode;
import com.yss.metadata.domain.collector.model.WorkflowNode;
import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 采集实例应用服务实现。
 */
@Service
@RequiredArgsConstructor
public class CollectorInstanceAppServiceImpl implements CollectorInstanceAppService {

    private final CollectorInstanceGateway collectorInstanceGateway;
    private final CollectorInstanceAppConvertor convertor = Mappers.getMapper(CollectorInstanceAppConvertor.class);

    @Override
    public List<CollectorInstanceVO> list(CollectorInstanceQuery query) {
        List<CollectorInstance> list = collectorInstanceGateway.findByQuery(query);
        return convertor.toVOList(list);
    }

    @Override
    public CollectorInstanceVO getById(String id) {
        CollectorInstance instance = findInstanceOrThrow(id);
        return convertor.toVO(instance);
    }

    @Override
    public MetadataDiffSummaryVO getDiffSummary(String id) {
        CollectorInstance instance = findInstanceOrThrow(id);
        if (instance.getDiffSummary() == null) {
            return null;
        }
        return convertor.toDiffVO(instance.getDiffSummary());
    }

    @Override
    public CollectorInstanceVO rerun(String id, CollectorInstanceRerunCmd cmd) {
        CollectorInstance instance = findInstanceOrThrow(id);
        String operator = cmd != null ? cmd.getOperator() : null;
        instance.rerun(operator);
        collectorInstanceGateway.save(instance);
        return convertor.toVO(instance);
    }

    @Override
    public List<CollectorInstanceVO> batchRerun(BatchInstanceCmd cmd) {
        return executeBatchOperation(
                cmd,
                inst -> inst.getStatus() == CollectorInstanceStatus.FAILED,
                inst -> inst.rerun(cmd.getOperator())
        );
    }

    @Override
    public CollectorInstanceVO terminate(String id, CollectorInstanceTerminateCmd cmd) {
        CollectorInstance instance = findInstanceOrThrow(id);
        String operator = cmd != null ? cmd.getOperator() : null;
        String reason = cmd != null ? cmd.getReason() : "用户手动终止";
        instance.terminate(operator, reason);
        collectorInstanceGateway.save(instance);
        return convertor.toVO(instance);
    }

    @Override
    public List<CollectorInstanceVO> batchTerminate(BatchInstanceCmd cmd) {
        return executeBatchOperation(
                cmd,
                inst -> inst.getStatus() == CollectorInstanceStatus.RUNNING || inst.getStatus() == CollectorInstanceStatus.PENDING,
                inst -> inst.terminate(cmd.getOperator(), cmd.getReason())
        );
    }

    @Override
    public List<WorkflowNodeVO> getWorkflowNodes(String instanceId) {
        CollectorInstance instance = findInstanceOrThrow(instanceId);
        return convertor.toNodeVOList(instance.getWorkflowNodes());
    }

    @Override
    public WorkflowNodeVO rerunWorkflowNode(String instanceId, String nodeId, String operator) {
        CollectorInstance instance = findInstanceOrThrow(instanceId);
        if (instance.getWorkflowNodes() == null) {
            throw new CollectorInstanceNotFoundException("未找到工作流节点: " + nodeId);
        }
        WorkflowNode target = instance.getWorkflowNodes().stream()
                .filter(n -> n.getId().equals(nodeId))
                .findFirst()
                .orElseThrow(() -> new CollectorInstanceNotFoundException("未找到工作流节点: " + nodeId));

        target.rerun();
        collectorInstanceGateway.save(instance);
        return convertor.toNodeVO(target);
    }

    private List<CollectorInstanceVO> executeBatchOperation(
            BatchInstanceCmd cmd,
            Predicate<CollectorInstance> condition,
            Consumer<CollectorInstance> action) {
        if (cmd == null || cmd.getInstanceIds() == null || cmd.getInstanceIds().isEmpty()) {
            return new ArrayList<>();
        }
        List<CollectorInstanceVO> results = new ArrayList<>();
        for (String id : cmd.getInstanceIds()) {
            CollectorInstance instance = collectorInstanceGateway.findById(id).orElse(null);
            if (instance != null && condition.test(instance)) {
                action.accept(instance);
                collectorInstanceGateway.save(instance);
                results.add(convertor.toVO(instance));
            }
        }
        return results;
    }

    private CollectorInstance findInstanceOrThrow(String id) {
        return collectorInstanceGateway.findById(id)
                .orElseThrow(() -> new CollectorInstanceNotFoundException(id));
    }
}
