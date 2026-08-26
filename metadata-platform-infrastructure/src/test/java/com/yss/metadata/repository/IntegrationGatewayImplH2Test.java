package com.yss.metadata.repository;

import com.yss.metadata.domain.integration.gateway.IntegrationConfigGateway;
import com.yss.metadata.domain.integration.gateway.OpenLineageEventGateway;
import com.yss.metadata.domain.integration.model.IntegrationConfig;
import com.yss.metadata.domain.integration.model.OpenLineageEventRecord;
import com.yss.metadata.domain.integration.model.OpenLineageParseStatus;
import com.yss.metadata.domain.integration.model.OpenLineageStats;
import com.yss.metadata.infrastructure.convertor.IntegrationConfigConvertor;
import com.yss.metadata.infrastructure.convertor.OpenLineageEventConvertor;
import com.yss.metadata.repository.entity.IntegrationConfigPO;
import com.yss.metadata.repository.entity.OpenLineageEventPO;
import com.yss.metadata.repository.gateway.impl.IntegrationConfigGatewayImpl;
import com.yss.metadata.repository.gateway.impl.OpenLineageEventGatewayImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 集成配置 / OpenLineage 事件仓储 H2 持久化测试（WU-05-01 / WU-05-02）。
 *
 * <p>覆盖：integration_config 单例行 upsert（insert→update 同 id=1）、
 * openlineage_event 追加记录 + 近 24h/解析成功率统计。</p>
 */
class IntegrationGatewayImplH2Test extends H2MapperTestSupport {

    private IntegrationConfigGateway configGateway;
    private OpenLineageEventGateway eventGateway;

    @BeforeEach
    void setUp() {
        configGateway = new IntegrationConfigGatewayImpl(
                sqlSession.getMapper(IntegrationConfigRepository.class),
                Mappers.getMapper(IntegrationConfigConvertor.class));
        eventGateway = new OpenLineageEventGatewayImpl(
                sqlSession.getMapper(OpenLineageEventRepository.class),
                Mappers.getMapper(OpenLineageEventConvertor.class));
    }

    @Test
    @DisplayName("无配置返回空（空结构非错误）")
    void findEmpty() {
        assertThat(configGateway.find()).isEmpty();
    }

    @Test
    @DisplayName("保存（insert）→ 读取；二次保存同 id=1 upsert 覆盖不新增行")
    void saveThenUpsertSameRow() {
        IntegrationConfig config = IntegrationConfig.builder()
                .id(IntegrationConfig.SINGLETON_ID)
                .gravitinoEndpoint("http://gravitino:8090")
                .gravitinoAuthRef("seam-base64:abc")
                .gravitinoEnabled(true)
                .datahubEndpoint("http://datahub:8080")
                .updatedAt(LocalDateTime.of(2026, 8, 12, 10, 0, 0))
                .build();
        configGateway.save(config);

        Optional<IntegrationConfig> found = configGateway.find();
        assertThat(found).isPresent();
        assertThat(found.get().getGravitinoEndpoint()).isEqualTo("http://gravitino:8090");
        assertThat(found.get().getGravitinoEnabled()).isTrue();
        assertThat(found.get().getDatahubEndpoint()).isEqualTo("http://datahub:8080");

        IntegrationConfig updated = IntegrationConfig.builder()
                .id(IntegrationConfig.SINGLETON_ID)
                .gravitinoEndpoint("http://gravitino-new:8090")
                .gravitinoEnabled(false)
                .build();
        configGateway.save(updated);

        assertThat(configGateway.find().get().getGravitinoEndpoint()).isEqualTo("http://gravitino-new:8090");
        assertThat(configGateway.find().get().getGravitinoEnabled()).isFalse();
        // 单例行：仍只有一行
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<IntegrationConfigPO> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        assertThat(sqlSession.getMapper(IntegrationConfigRepository.class).selectCount(wrapper)).isEqualTo(1);
    }

    @Test
    @DisplayName("事件追加记录：字段完整落库（parse_status 值契约）")
    void saveEventPersists() {
        OpenLineageEventRecord record = OpenLineageEventRecord.builder()
                .id("evt-1")
                .eventType("COMPLETE")
                .eventTime(LocalDateTime.of(2026, 8, 12, 10, 0, 0))
                .runId("run-1")
                .jobNamespace("ns1")
                .jobName("job1")
                .parseStatus(OpenLineageParseStatus.PARSED)
                .receivedAt(LocalDateTime.now())
                .build();
        eventGateway.save(record);

        OpenLineageEventPO po = sqlSession.getMapper(OpenLineageEventRepository.class).selectById("evt-1");
        assertThat(po).isNotNull();
        assertThat(po.getEventType()).isEqualTo("COMPLETE");
        assertThat(po.getRunId()).isEqualTo("run-1");
        assertThat(po.getJobNamespace()).isEqualTo("ns1");
        assertThat(po.getJobName()).isEqualTo("job1");
        assertThat(po.getParseStatus()).isEqualTo("parsed");
        assertThat(po.getReceivedAt()).isNotNull();
    }

    @Test
    @DisplayName("统计：近 24h 事件数与解析成功率（parsed/总数；0 事件空结构）")
    void statsAggregates() {
        OpenLineageStats empty = eventGateway.stats();
        assertThat(empty.getRecent24hCount()).isZero();
        assertThat(empty.getParseSuccessRate()).isZero();

        eventGateway.save(record("e1", OpenLineageParseStatus.PARSED, LocalDateTime.now()));
        eventGateway.save(record("e2", OpenLineageParseStatus.PARSED, LocalDateTime.now()));
        eventGateway.save(record("e3", OpenLineageParseStatus.PARSE_FAILED, LocalDateTime.now()));
        // 24h 窗口外的历史事件不计入近 24h，但计入成功率分母
        eventGateway.save(record("e4", OpenLineageParseStatus.PARSED,
                LocalDateTime.now().minusHours(30)));

        OpenLineageStats stats = eventGateway.stats();
        assertThat(stats.getRecent24hCount()).isEqualTo(3);
        assertThat(stats.getParseSuccessRate()).isEqualTo(0.75);
    }

    private OpenLineageEventRecord record(String id, OpenLineageParseStatus status, LocalDateTime receivedAt) {
        return OpenLineageEventRecord.builder()
                .id(id)
                .eventType("COMPLETE")
                .runId("run-" + id)
                .jobNamespace("ns1")
                .jobName("job1")
                .parseStatus(status)
                .receivedAt(receivedAt)
                .build();
    }
}
