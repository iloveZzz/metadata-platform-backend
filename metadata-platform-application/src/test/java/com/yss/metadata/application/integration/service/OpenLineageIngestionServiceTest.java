package com.yss.metadata.application.integration.service;

import com.yss.metadata.application.collector.support.InMemoryAssetGateway;
import com.yss.metadata.application.integration.service.impl.OpenLineageIngestionServiceImpl;
import com.yss.metadata.application.integration.support.InMemoryOpenLineageEventGateway;
import com.yss.metadata.application.lineage.support.InMemoryLineageGraphRepository;
import com.yss.metadata.client.dto.cmd.OpenLineageDatasetCmd;
import com.yss.metadata.client.dto.cmd.OpenLineageEventCmd;
import com.yss.metadata.client.dto.cmd.OpenLineageJobCmd;
import com.yss.metadata.client.dto.cmd.OpenLineageRunCmd;
import com.yss.metadata.domain.integration.model.OpenLineageEventRecord;
import com.yss.metadata.domain.integration.model.OpenLineageEventType;
import com.yss.metadata.domain.integration.model.OpenLineageParseStatus;
import com.yss.metadata.domain.lineage.model.LineageConfidence;
import com.yss.metadata.domain.lineage.model.LineageEdge;
import com.yss.metadata.domain.lineage.model.LineageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OpenLineage 事件接收应用服务测试（WU-05-02）。
 *
 * <p>覆盖：RunEvent 子集校验 422、解析→资产入库（source_id=namespace）+ COMPLETE
 * 血缘边（type=job / auto-high）、事件记录与解析状态、无数据集 parse_failed、
 * 重复边去重。</p>
 */
class OpenLineageIngestionServiceTest {

    private InMemoryOpenLineageEventGateway eventGateway;
    private InMemoryAssetGateway assetGateway;
    private InMemoryLineageGraphRepository graphRepository;
    private OpenLineageIngestionService service;

    @BeforeEach
    void setUp() {
        eventGateway = new InMemoryOpenLineageEventGateway();
        assetGateway = new InMemoryAssetGateway();
        graphRepository = new InMemoryLineageGraphRepository();
        service = new OpenLineageIngestionServiceImpl(eventGateway, assetGateway, graphRepository);
    }

    @Test
    @DisplayName("COMPLETE 事件：数据集映射资产（namespace→source_id）+ 血缘边 inputs→outputs + 事件记录 PARSED")
    void completeEventIngestsAssetsAndEdges() {
        service.receive(cmd(OpenLineageEventType.COMPLETE, "run-1", "ns1", "job1",
                java.util.Collections.singletonList(dataset("ns1", "ods_order")),
                java.util.Collections.singletonList(dataset("ns2", "dwd_order_di"))));

        // 资产入库：两个数据集，source_id=namespace
        assertThat(assetGateway.getSaved()).hasSize(2);
        assertThat(assetGateway.getSaved()).anySatisfy(batch ->
                assertThat(batch.getSourceId()).isEqualTo("ns1"));
        assertThat(assetGateway.getSaved()).extracting(batch -> batch.getAssets().get(0).getName())
                .containsExactlyInAnyOrder("ods_order", "dwd_order_di");
        // 血缘边：job 类型 + auto-high
        assertThat(graphRepository.allEdges()).hasSize(1);
        LineageEdge edge = graphRepository.allEdges().get(0);
        assertThat(edge.getFromAssetId()).isEqualTo("asset-ods_order");
        assertThat(edge.getToAssetId()).isEqualTo("asset-dwd_order_di");
        assertThat(edge.getType()).isEqualTo(LineageType.JOB);
        assertThat(edge.getConfidence()).isEqualTo(LineageConfidence.AUTO_HIGH);
        // 事件记录
        assertThat(eventGateway.all()).hasSize(1);
        OpenLineageEventRecord record = eventGateway.all().get(0);
        assertThat(record.getEventType()).isEqualTo("COMPLETE");
        assertThat(record.getRunId()).isEqualTo("run-1");
        assertThat(record.getJobNamespace()).isEqualTo("ns1");
        assertThat(record.getJobName()).isEqualTo("job1");
        assertThat(record.getParseStatus()).isEqualTo(OpenLineageParseStatus.PARSED);
    }

    @Test
    @DisplayName("无数据集事件：记录 parse_failed（非错误），不写资产不写血缘")
    void eventWithoutDatasetsMarkedParseFailed() {
        service.receive(cmd(OpenLineageEventType.START, "run-2", "ns1", "job2"));

        assertThat(assetGateway.getSaved()).isEmpty();
        assertThat(graphRepository.allEdges()).isEmpty();
        assertThat(eventGateway.all()).hasSize(1);
        assertThat(eventGateway.all().get(0).getParseStatus())
                .isEqualTo(OpenLineageParseStatus.PARSE_FAILED);
    }

    @Test
    @DisplayName("全非法数据集：跳过非法项，合法项仍入库并 PARSED")
    void invalidDatasetsSkipped() {
        OpenLineageEventCmd event = cmd(OpenLineageEventType.COMPLETE, "run-3", "ns1", "job3",
                java.util.Collections.singletonList(dataset("ns1", "ods_order")),
                java.util.Collections.singletonList(dataset("", "")));
        service.receive(event);

        assertThat(assetGateway.getSaved()).hasSize(1);
        assertThat(assetGateway.getSaved().get(0).getAssets().get(0).getName()).isEqualTo("ods_order");
        assertThat(eventGateway.all().get(0).getParseStatus()).isEqualTo(OpenLineageParseStatus.PARSED);
    }

    @Test
    @DisplayName("重复血缘边去重：同 inputs→outputs 重复事件只写一条边")
    void duplicateEdgesDeduped() {
        service.receive(cmd(OpenLineageEventType.COMPLETE, "run-1", "ns1", "job1",
                java.util.Collections.singletonList(dataset("ns1", "a")),
                java.util.Collections.singletonList(dataset("ns2", "b"))));
        service.receive(cmd(OpenLineageEventType.COMPLETE, "run-1", "ns1", "job1",
                java.util.Collections.singletonList(dataset("ns1", "a")),
                java.util.Collections.singletonList(dataset("ns2", "b"))));

        assertThat(graphRepository.allEdges()).hasSize(1);
        assertThat(eventGateway.all()).hasSize(2);
    }

    @Test
    @DisplayName("缺 eventType 抛非法参数（422 语义），不落记录")
    void missingEventTypeThrows() {
        OpenLineageEventCmd event = cmd(null, "run-1", "ns1", "job1");
        assertThatThrownBy(() -> service.receive(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eventType");
        assertThat(eventGateway.all()).isEmpty();
    }

    @Test
    @DisplayName("缺 run.runId 抛非法参数（422 语义）")
    void missingRunIdThrows() {
        OpenLineageEventCmd event = new OpenLineageEventCmd();
        event.setEventType(OpenLineageEventType.COMPLETE);
        event.setJob(job("ns1", "job1"));
        assertThatThrownBy(() -> service.receive(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("run.runId");
    }

    @Test
    @DisplayName("缺 job.namespace / job.name 抛非法参数（422 语义）")
    void missingJobThrows() {
        OpenLineageEventCmd event = new OpenLineageEventCmd();
        event.setEventType(OpenLineageEventType.COMPLETE);
        event.setRun(run("run-1"));
        event.setJob(job("", ""));
        assertThatThrownBy(() -> service.receive(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("job");
    }

    @Test
    @DisplayName("解析异常（资产入库抛错）不阻断事件接收：记录 parse_failed")
    void ingestExceptionRecordedAsParseFailed() {
        assetGateway.setFailure(new IllegalStateException("入库失败"));
        service.receive(cmd(OpenLineageEventType.COMPLETE, "run-4", "ns1", "job4",
                java.util.Collections.singletonList(dataset("ns1", "a")),
                java.util.Collections.singletonList(dataset("ns2", "b"))));

        assertThat(eventGateway.all()).hasSize(1);
        assertThat(eventGateway.all().get(0).getParseStatus())
                .isEqualTo(OpenLineageParseStatus.PARSE_FAILED);
    }

    private OpenLineageEventCmd cmd(OpenLineageEventType type, String runId, String namespace,
                                    String jobName, List<OpenLineageDatasetCmd> inputs,
                                    List<OpenLineageDatasetCmd> outputs) {
        OpenLineageEventCmd event = new OpenLineageEventCmd();
        event.setEventType(type);
        event.setEventTime(LocalDateTime.of(2026, 8, 12, 10, 0, 0));
        event.setRun(run(runId));
        event.setJob(job(namespace, jobName));
        event.setInputs(inputs);
        event.setOutputs(outputs);
        return event;
    }

    private OpenLineageEventCmd cmd(OpenLineageEventType type, String runId, String namespace,
                                    String jobName) {
        return cmd(type, runId, namespace, jobName,
                java.util.Collections.emptyList(), java.util.Collections.emptyList());
    }

    private OpenLineageRunCmd run(String runId) {
        OpenLineageRunCmd run = new OpenLineageRunCmd();
        run.setRunId(runId);
        return run;
    }

    private OpenLineageJobCmd job(String namespace, String name) {
        OpenLineageJobCmd job = new OpenLineageJobCmd();
        job.setNamespace(namespace);
        job.setName(name);
        return job;
    }

    private OpenLineageDatasetCmd dataset(String namespace, String name) {
        OpenLineageDatasetCmd dataset = new OpenLineageDatasetCmd();
        dataset.setNamespace(namespace);
        dataset.setName(name);
        return dataset;
    }
}
