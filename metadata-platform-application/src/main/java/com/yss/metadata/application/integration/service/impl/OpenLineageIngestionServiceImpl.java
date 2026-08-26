package com.yss.metadata.application.integration.service.impl;

import com.yss.metadata.application.integration.service.OpenLineageIngestionService;
import com.yss.metadata.client.dto.cmd.OpenLineageDatasetCmd;
import com.yss.metadata.client.dto.cmd.OpenLineageEventCmd;
import com.yss.metadata.domain.collector.gateway.AssetGateway;
import com.yss.metadata.domain.collector.model.CollectedAsset;
import com.yss.metadata.domain.collector.model.SavedAssetRef;
import com.yss.metadata.domain.integration.gateway.OpenLineageEventGateway;
import com.yss.metadata.domain.integration.model.OpenLineageDataset;
import com.yss.metadata.domain.integration.model.OpenLineageEvent;
import com.yss.metadata.domain.integration.model.OpenLineageEventRecord;
import com.yss.metadata.domain.integration.model.OpenLineageEventType;
import com.yss.metadata.domain.integration.model.OpenLineageParseStatus;
import com.yss.metadata.domain.lineage.gateway.LineageGraphRepository;
import com.yss.metadata.domain.lineage.model.LineageConfidence;
import com.yss.metadata.domain.lineage.model.LineageEdge;
import com.yss.metadata.domain.lineage.model.LineageGraph;
import com.yss.metadata.domain.lineage.model.LineageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * OpenLineage 事件接收应用服务实现（WU-05-02）。
 *
 * <p>校验（eventType/runId/job 必填 → 422）→ 事件记录（parse_status 承载
 * parsed/parse_failed）→ 数据集映射资产（source_id=namespace、name=dataset name，
 * 复用 AssetGateway 幂等 upsert）+ COMPLETE 事件血缘边（inputs→outputs，
 * type=job，confidence=auto-high，去重）。</p>
 *
 * <p>受控解读（代码注释登记）：事件数据集按 namespace/name 映射资产与血缘端点
 * （原始事件不回存，标准化子集入库）；facets 列级解析 seam-deferred；
 * run_id 幂等完备化 seam-deferred（当前追加记录 + 资产/血缘幂等 upsert）。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenLineageIngestionServiceImpl implements OpenLineageIngestionService {

    private final OpenLineageEventGateway openLineageEventGateway;
    private final AssetGateway assetGateway;
    private final LineageGraphRepository lineageGraphRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receive(OpenLineageEventCmd cmd) {
        OpenLineageEvent event = toEvent(cmd);
        OpenLineageParseStatus status;
        try {
            status = ingest(event) ? OpenLineageParseStatus.PARSED : OpenLineageParseStatus.PARSE_FAILED;
        } catch (RuntimeException e) {
            log.warn("OpenLineage 事件解析失败，runId={}, job={}.{}", event.getRunId(),
                    event.getJobNamespace(), event.getJobName(), e);
            status = OpenLineageParseStatus.PARSE_FAILED;
        }
        openLineageEventGateway.save(OpenLineageEventRecord.builder()
                .id(UUID.randomUUID().toString())
                .eventType(event.getEventType() == null ? null : event.getEventType().getValue())
                .eventTime(event.getEventTime())
                .runId(event.getRunId())
                .jobNamespace(event.getJobNamespace())
                .jobName(event.getJobName())
                .parseStatus(status)
                .receivedAt(LocalDateTime.now())
                .build());
        log.info("OpenLineage 事件已接收，runId={}, eventType={}, parseStatus={}",
                event.getRunId(), event.getEventType(), status);
    }

    /**
     * 事件 → 领域模型 + 必填校验（eventType/runId/job 非空；非法抛非法参数 → 422）。
     */
    private OpenLineageEvent toEvent(OpenLineageEventCmd cmd) {
        if (cmd == null || cmd.getEventType() == null) {
            throw new IllegalArgumentException("OpenLineage 事件缺少 eventType");
        }
        if (cmd.getRun() == null || !StringUtils.hasText(cmd.getRun().getRunId())) {
            throw new IllegalArgumentException("OpenLineage 事件缺少 run.runId");
        }
        if (cmd.getJob() == null || !StringUtils.hasText(cmd.getJob().getNamespace())
                || !StringUtils.hasText(cmd.getJob().getName())) {
            throw new IllegalArgumentException("OpenLineage 事件缺少 job.namespace / job.name");
        }
        return OpenLineageEvent.builder()
                .eventType(cmd.getEventType())
                .eventTime(cmd.getEventTime())
                .runId(cmd.getRun().getRunId())
                .jobNamespace(cmd.getJob().getNamespace())
                .jobName(cmd.getJob().getName())
                .inputs(toDatasets(cmd.getInputs()))
                .outputs(toDatasets(cmd.getOutputs()))
                .build();
    }

    private List<OpenLineageDataset> toDatasets(List<OpenLineageDatasetCmd> cmds) {
        List<OpenLineageDataset> datasets = new ArrayList<>();
        if (cmds == null) {
            return datasets;
        }
        for (OpenLineageDatasetCmd cmd : cmds) {
            if (cmd != null && StringUtils.hasText(cmd.getNamespace()) && StringUtils.hasText(cmd.getName())) {
                datasets.add(OpenLineageDataset.builder()
                        .namespace(cmd.getNamespace())
                        .name(cmd.getName())
                        .build());
            }
        }
        return datasets;
    }

    /**
     * 数据入库：全量数据集 upsert 资产 + COMPLETE 事件写血缘边。
     *
     * @return 是否有数据集可写入（无数据集/全非法 → false = parse_failed）
     */
    private boolean ingest(OpenLineageEvent event) {
        List<OpenLineageDataset> inputs = event.getInputs() == null ? Collections.emptyList() : event.getInputs();
        List<OpenLineageDataset> outputs = event.getOutputs() == null ? Collections.emptyList() : event.getOutputs();
        if (inputs.isEmpty() && outputs.isEmpty()) {
            return false;
        }
        // 数据集去重 upsert 资产（source_id=namespace、name=dataset name）
        Map<String, String> datasetAssetIds = new LinkedHashMap<>();
        for (OpenLineageDataset dataset : concat(inputs, outputs)) {
            String key = dataset.getNamespace() + "::" + dataset.getName();
            if (!datasetAssetIds.containsKey(key)) {
                datasetAssetIds.put(key, saveDatasetAsset(dataset));
            }
        }
        // COMPLETE 事件：inputs → outputs 血缘边（去重：同 from+to+type 已存在跳过）
        if (event.getEventType() == OpenLineageEventType.COMPLETE && !inputs.isEmpty() && !outputs.isEmpty()) {
            LineageGraph graph = lineageGraphRepository.loadGraph();
            for (OpenLineageDataset input : inputs) {
                for (OpenLineageDataset output : outputs) {
                    writeEdge(graph, datasetAssetIds.get(input.getNamespace() + "::" + input.getName()),
                            datasetAssetIds.get(output.getNamespace() + "::" + output.getName()), event);
                }
            }
        }
        return true;
    }

    private String saveDatasetAsset(OpenLineageDataset dataset) {
        List<SavedAssetRef> saved = assetGateway.saveAssets(dataset.getNamespace(),
                Collections.singletonList(CollectedAsset.builder().name(dataset.getName()).type("table").build()));
        if (saved.isEmpty()) {
            throw new IllegalStateException("数据集资产入库失败: " + dataset.getNamespace() + "." + dataset.getName());
        }
        return saved.get(0).getAssetId();
    }

    private void writeEdge(LineageGraph graph, String fromAssetId, String toAssetId, OpenLineageEvent event) {
        if (fromAssetId == null || toAssetId == null) {
            return;
        }
        boolean exists = graph.getEdges().stream()
                .anyMatch(edge -> fromAssetId.equals(edge.getFromAssetId())
                        && toAssetId.equals(edge.getToAssetId())
                        && edge.getType() == LineageType.JOB);
        if (exists) {
            return;
        }
        // graphVersion 语义：ol-<eventTime>；事件时间缺失时不产 "ol-null"（置空，图版本取边版本最大值）
        String graphVersion = event.getEventTime() == null ? null : "ol-" + event.getEventTime();
        lineageGraphRepository.save(LineageEdge.builder()
                .fromAssetId(fromAssetId)
                .toAssetId(toAssetId)
                .type(LineageType.JOB)
                .confidence(LineageConfidence.AUTO_HIGH)
                .remark("OpenLineage 事件: " + event.getJobNamespace() + "." + event.getJobName())
                .graphVersion(graphVersion)
                .build());
    }

    private List<OpenLineageDataset> concat(List<OpenLineageDataset> a, List<OpenLineageDataset> b) {
        List<OpenLineageDataset> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }
}
