package com.yss.metadata.domain.integration;

import com.yss.metadata.domain.integration.model.OpenLineageDataset;
import com.yss.metadata.domain.integration.model.OpenLineageEvent;
import com.yss.metadata.domain.integration.model.OpenLineageEventRecord;
import com.yss.metadata.domain.integration.model.OpenLineageEventType;
import com.yss.metadata.domain.integration.model.OpenLineageParseStatus;
import com.yss.metadata.domain.integration.model.OpenLineageStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OpenLineage 领域模型测试（WU-05-02）。
 *
 * <p>覆盖：事件类型枚举 JSON 值解析（非法值抛错→422 语义）、事件/记录/统计模型构建。</p>
 */
class OpenLineageEventTest {

    @Test
    @DisplayName("事件类型枚举值契约：START/COMPLETE/FAIL/ABORT ↔ 列值")
    void eventTypeValueContract() {
        assertThat(OpenLineageEventType.fromValue("START")).isEqualTo(OpenLineageEventType.START);
        assertThat(OpenLineageEventType.fromValue("COMPLETE")).isEqualTo(OpenLineageEventType.COMPLETE);
        assertThat(OpenLineageEventType.fromValue("FAIL")).isEqualTo(OpenLineageEventType.FAIL);
        assertThat(OpenLineageEventType.fromValue("ABORT")).isEqualTo(OpenLineageEventType.ABORT);
        assertThat(OpenLineageEventType.fromValue(null)).isNull();
        assertThat(OpenLineageEventType.START.getValue()).isEqualTo("START");
    }

    @Test
    @DisplayName("未知事件类型抛非法参数（Web 层映射 422）")
    void unknownEventTypeThrows() {
        assertThatThrownBy(() -> OpenLineageEventType.fromValue("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    @DisplayName("事件模型构建：inputs/outputs 缺省空列表，可承载 run/job/数据集")
    void eventBuild() {
        OpenLineageEvent event = OpenLineageEvent.builder()
                .eventType(OpenLineageEventType.COMPLETE)
                .eventTime(LocalDateTime.of(2026, 8, 12, 10, 0, 0))
                .runId("run-1")
                .jobNamespace("ns1")
                .jobName("job1")
                .inputs(Collections.singletonList(OpenLineageDataset.builder()
                        .namespace("ns1").name("ods_order").build()))
                .outputs(Collections.singletonList(OpenLineageDataset.builder()
                        .namespace("ns2").name("dwd_order_di").build()))
                .build();

        assertThat(event.getEventType()).isEqualTo(OpenLineageEventType.COMPLETE);
        assertThat(event.getRunId()).isEqualTo("run-1");
        assertThat(event.getInputs()).hasSize(1);
        assertThat(event.getInputs().get(0).getNamespace()).isEqualTo("ns1");
        assertThat(event.getInputs().get(0).getName()).isEqualTo("ods_order");
        assertThat(event.getOutputs().get(0).getName()).isEqualTo("dwd_order_di");
    }

    @Test
    @DisplayName("事件记录模型：解析状态 parsed/parse_failed 值契约")
    void recordParseStatusContract() {
        assertThat(OpenLineageParseStatus.PARSED.getValue()).isEqualTo("parsed");
        assertThat(OpenLineageParseStatus.PARSE_FAILED.getValue()).isEqualTo("parse_failed");

        OpenLineageEventRecord record = OpenLineageEventRecord.builder()
                .id("evt-1")
                .eventType("COMPLETE")
                .runId("run-1")
                .jobNamespace("ns1")
                .jobName("job1")
                .parseStatus(OpenLineageParseStatus.PARSED)
                .receivedAt(LocalDateTime.now())
                .build();

        assertThat(record.getParseStatus()).isEqualTo(OpenLineageParseStatus.PARSED);
        assertThat(record.getEventType()).isEqualTo("COMPLETE");
    }

    @Test
    @DisplayName("事件统计模型：近 24h 计数与解析成功率承载")
    void statsModel() {
        OpenLineageStats stats = OpenLineageStats.builder()
                .recent24hCount(42)
                .parseSuccessRate(0.976)
                .build();

        assertThat(stats.getRecent24hCount()).isEqualTo(42);
        assertThat(stats.getParseSuccessRate()).isEqualTo(0.976);
    }
}
