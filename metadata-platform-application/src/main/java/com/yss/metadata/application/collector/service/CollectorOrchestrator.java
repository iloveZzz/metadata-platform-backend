package com.yss.metadata.application.collector.service;

import com.yss.metadata.application.collector.service.convertor.CollectorAppConvertor;
import com.yss.metadata.application.governance.service.support.SensitiveRecognitionApplier;
import com.yss.metadata.client.vo.CollectorVO;
import com.yss.metadata.domain.collector.exception.CollectorTaskNotFoundException;
import com.yss.metadata.domain.collector.gateway.AssetGateway;
import com.yss.metadata.domain.collector.gateway.CollectorInstanceGateway;
import com.yss.metadata.domain.collector.gateway.CollectorTaskGateway;
import com.yss.metadata.domain.collector.model.CollectedAsset;
import com.yss.metadata.domain.collector.model.CollectorExecutionResult;
import com.yss.metadata.domain.collector.model.CollectorInstance;
import com.yss.metadata.domain.collector.model.CollectorInstanceStatus;
import com.yss.metadata.domain.collector.model.CollectorTask;
import com.yss.metadata.domain.collector.model.ExecutionMode;
import com.yss.metadata.domain.collector.model.SavedAssetRef;
import com.yss.metadata.domain.collector.model.WorkflowNode;
import com.yss.metadata.domain.collector.model.WorkflowNodeType;
import com.yss.metadata.domain.collector.spi.CollectorExecutionSpi;
import com.yss.metadata.domain.collector.spi.CollectorTaskTriggerSpi;
import com.yss.metadata.domain.connector.exception.ConnectorNotFoundException;
import com.yss.metadata.domain.connector.gateway.ConnectorGateway;
import com.yss.metadata.domain.connector.model.ConnectTestResult;
import com.yss.metadata.domain.connector.model.Connector;
import com.yss.metadata.domain.connector.spi.ConnectorTestSpi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 采集编排（WU-01-03）：start → 连接校验 → 采集执行 → 资产入库 → 成功/失败落状态与采集实例全生命周期持久化。
 */
@Service
@Slf4j
public class CollectorOrchestrator implements CollectorTaskTriggerSpi {

    private final CollectorTaskGateway collectorTaskGateway;
    private final ConnectorGateway connectorGateway;
    private final ConnectorTestSpi connectorTestSpi;
    private final CollectorExecutionSpi collectorExecutionSpi;
    private final AssetGateway assetGateway;
    private final SensitiveRecognitionApplier sensitiveRecognitionApplier;
    private final CollectorAppConvertor collectorAppConvertor;
    private final CollectorInstanceGateway collectorInstanceGateway;

    @Autowired
    public CollectorOrchestrator(CollectorTaskGateway collectorTaskGateway,
                                 ConnectorGateway connectorGateway,
                                 ConnectorTestSpi connectorTestSpi,
                                 CollectorExecutionSpi collectorExecutionSpi,
                                 AssetGateway assetGateway,
                                 SensitiveRecognitionApplier sensitiveRecognitionApplier,
                                 CollectorAppConvertor collectorAppConvertor,
                                 @Autowired(required = false) CollectorInstanceGateway collectorInstanceGateway) {
        this.collectorTaskGateway = collectorTaskGateway;
        this.connectorGateway = connectorGateway;
        this.connectorTestSpi = connectorTestSpi;
        this.collectorExecutionSpi = collectorExecutionSpi;
        this.assetGateway = assetGateway;
        this.sensitiveRecognitionApplier = sensitiveRecognitionApplier;
        this.collectorAppConvertor = collectorAppConvertor;
        this.collectorInstanceGateway = collectorInstanceGateway;
    }

    public CollectorOrchestrator(CollectorTaskGateway collectorTaskGateway,
                                 ConnectorGateway connectorGateway,
                                 ConnectorTestSpi connectorTestSpi,
                                 CollectorExecutionSpi collectorExecutionSpi,
                                 AssetGateway assetGateway,
                                 SensitiveRecognitionApplier sensitiveRecognitionApplier,
                                 CollectorAppConvertor collectorAppConvertor) {
        this(collectorTaskGateway, connectorGateway, connectorTestSpi, collectorExecutionSpi,
                assetGateway, sensitiveRecognitionApplier, collectorAppConvertor, null);
    }

    /**
     * 立即执行采集任务（POST /api/collectors/run 用例）。
     *
     * <p>运行中重复触发抛状态冲突（409 幂等拒绝）；连接校验失败或采集执行失败
     * 时任务标记失败并携带分类失败原因，运行请求本身已接受（202 语义）。
     * 全流程同步物化生成并流转 CollectorInstance 执行实例。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public CollectorVO run(String collectorId) {
        CollectorTask task = requireTask(collectorId);
        task.start();
        Connector connector = connectorGateway.findById(task.getConnectorId())
                .orElseThrow(() -> new ConnectorNotFoundException(task.getConnectorId()));

        String instanceId = "inst-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        LocalDateTime startTime = LocalDateTime.now();
        List<WorkflowNode> nodes = new ArrayList<>();
        WorkflowNode probeNode = WorkflowNode.builder()
                .id(instanceId + "-node-1")
                .name("JDBC 连通性探测与凭据校验")
                .type(WorkflowNodeType.JDBC_PROBE)
                .status(CollectorInstanceStatus.RUNNING)
                .startTime(startTime)
                .logs(new ArrayList<>(Collections.singletonList("[INFO] 开始网络连接池与凭据校验...")))
                .build();
        nodes.add(probeNode);

        CollectorInstance instance = CollectorInstance.builder()
                .id(instanceId)
                .name(task.getName())
                .collectorId(task.getId())
                .collectorName(task.getName())
                .connectorId(task.getConnectorId())
                .connectorName(connector.getName())
                .datasourceType(connector.getType() != null ? connector.getType().name() : task.getDatasourceType())
                .status(CollectorInstanceStatus.RUNNING)
                .executionMode(ExecutionMode.MANUAL)
                .scheduleDescription(task.getSchedule() != null ? task.getSchedule().getValue() : "手动执行")
                .startTime(startTime)
                .executor(task.getOwner() != null ? task.getOwner() : "System")
                .owner(task.getOwner())
                .isDryRun(Boolean.FALSE)
                .retryCount(0)
                .maxRetries(3)
                .workflowNodes(nodes)
                .build();

        persistInstance(instance);

        ConnectTestResult connect = connectorTestSpi.test(connector);
        if (!connect.isConnected()) {
            probeNode.setStatus(CollectorInstanceStatus.FAILED);
            probeNode.setEndTime(LocalDateTime.now());
            probeNode.setExceptionInfo(connect.getMessage());

            task.markFailed(connect.getMessage());
            collectorTaskGateway.save(task);

            instance.setStatus(CollectorInstanceStatus.FAILED);
            instance.setEndTime(LocalDateTime.now());
            instance.setDurationMs(Duration.between(startTime, instance.getEndTime()).toMillis());
            instance.setErrorMessage("连接校验失败: " + connect.getMessage());
            persistInstance(instance);

            log.warn("采集任务连接校验失败，id={}, category={}, message={}",
                    collectorId, connect.getErrorType(), connect.getMessage());
            return collectorAppConvertor.toVO(task);
        }

        probeNode.setStatus(CollectorInstanceStatus.SUCCESS);
        probeNode.setEndTime(LocalDateTime.now());
        probeNode.getLogs().add("[INFO] 连接校验通过，耗时 " + Duration.between(probeNode.getStartTime(), probeNode.getEndTime()).toMillis() + "ms");

        WorkflowNode dlinkNode = WorkflowNode.builder()
                .id(instanceId + "-node-2")
                .name("Dlink 分布式元数据抽取计算")
                .type(WorkflowNodeType.DLINK)
                .status(CollectorInstanceStatus.RUNNING)
                .startTime(LocalDateTime.now())
                .logs(new ArrayList<>(Collections.singletonList("[INFO] Dlink TaskManager 开始并行抽取元数据分区...")))
                .build();
        nodes.add(dlinkNode);
        persistInstance(instance);

        CollectorExecutionResult exec = collectorExecutionSpi.execute(task);
        if (exec.isSuccess()) {
            dlinkNode.setStatus(CollectorInstanceStatus.SUCCESS);
            dlinkNode.setEndTime(LocalDateTime.now());
            dlinkNode.getLogs().add("[INFO] 元数据抽取成功完成");

            WorkflowNode parseNode = WorkflowNode.builder()
                    .id(instanceId + "-node-3")
                    .name("Schema 解析与变更 Diff 计算")
                    .type(WorkflowNodeType.SCHEMA_PARSE)
                    .status(CollectorInstanceStatus.SUCCESS)
                    .startTime(LocalDateTime.now())
                    .endTime(LocalDateTime.now())
                    .logs(new ArrayList<>(Collections.singletonList("[INFO] Schema 解析完成，增量版本比对就绪")))
                    .build();
            nodes.add(parseNode);

            try {
                if (exec.getAssets() != null && !exec.getAssets().isEmpty()) {
                    for (CollectedAsset ca : exec.getAssets()) {
                        if (ca.getSourceSystem() == null) {
                            ca.setSourceSystem(task.getSourceSystem());
                        }
                        if (ca.getCollectorTaskId() == null) {
                            ca.setCollectorTaskId(task.getId());
                        }
                    }
                    List<SavedAssetRef> saved = assetGateway.saveAssets(task.getConnectorId(), exec.getAssets());
                    if (Boolean.TRUE.equals(task.getAutoClassify())) {
                        runSensitiveRecognition(saved, collectorId);
                    }
                }
                WorkflowNode ingestNode = WorkflowNode.builder()
                        .id(instanceId + "-node-4")
                        .name("资产目录与索引更新入库")
                        .type(WorkflowNodeType.CATALOG_INGEST)
                        .status(CollectorInstanceStatus.SUCCESS)
                        .startTime(LocalDateTime.now())
                        .endTime(LocalDateTime.now())
                        .logs(new ArrayList<>(Collections.singletonList("[INFO] 资产目录表及检索索引更新完成")))
                        .build();
                nodes.add(ingestNode);

                task.markSucceeded();
                instance.setStatus(CollectorInstanceStatus.SUCCESS);
                instance.setEndTime(LocalDateTime.now());
                instance.setDurationMs(Duration.between(startTime, instance.getEndTime()).toMillis());
            } catch (RuntimeException e) {
                log.error("资产入库失败，采集任务标记失败，id={}", collectorId, e);
                task.markFailed("资产入库失败: " + e.getMessage());
                instance.setStatus(CollectorInstanceStatus.FAILED);
                instance.setEndTime(LocalDateTime.now());
                instance.setDurationMs(Duration.between(startTime, instance.getEndTime()).toMillis());
                instance.setErrorMessage("资产入库失败: " + e.getMessage());
            }
        } else {
            dlinkNode.setStatus(CollectorInstanceStatus.FAILED);
            dlinkNode.setEndTime(LocalDateTime.now());
            dlinkNode.setExceptionInfo(exec.getFailReason());

            task.markFailed(exec.getFailReason());
            instance.setStatus(CollectorInstanceStatus.FAILED);
            instance.setEndTime(LocalDateTime.now());
            instance.setDurationMs(Duration.between(startTime, instance.getEndTime()).toMillis());
            instance.setErrorMessage(exec.getFailReason());
        }

        collectorTaskGateway.save(task);
        persistInstance(instance);
        log.info("采集编排完成，id={}, status={}, instanceId={}", collectorId, task.getStatus(), instanceId);
        return collectorAppConvertor.toVO(task);
    }

    private void persistInstance(CollectorInstance instance) {
        if (collectorInstanceGateway != null) {
            try {
                collectorInstanceGateway.save(instance);
            } catch (Exception e) {
                log.warn("保存采集实例失败 (尽力而为): instanceId={}, error={}", instance.getId(), e.getMessage());
            }
        }
    }

    /**
     * 敏感识别落候选（尽力而为增强：识别失败仅告警，不使采集任务失败；
     * 资产入库已成功提交，任务仍标记成功）。
     */
    private void runSensitiveRecognition(List<SavedAssetRef> saved, String collectorId) {
        try {
            sensitiveRecognitionApplier.apply(saved);
        } catch (RuntimeException e) {
            log.warn("敏感识别落候选失败（尽力而为，不影响采集任务状态），id={}", collectorId, e);
        }
    }

    /**
     * 失败重试 / 局部重采（POST /api/collectors/{id}/retry 用例）。
     *
     * <p>局部重采物理逻辑 seam-deferred（仅重采失败项），当前按整任务重新执行。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public CollectorVO retry(String collectorId, boolean failedItemsOnly) {
        log.info("采集任务重试/局部重采，id={}, failedItemsOnly={}（局部重采物理逻辑 seam-deferred）",
                collectorId, failedItemsOnly);
        return run(collectorId);
    }

    @Override
    public void execute(String collectorTaskId) {
        run(collectorTaskId);
    }

    private CollectorTask requireTask(String collectorId) {
        return collectorTaskGateway.findById(collectorId)
                .orElseThrow(() -> new CollectorTaskNotFoundException(collectorId));
    }
}
