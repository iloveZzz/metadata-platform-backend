package com.yss.metadata.repository;

import com.yss.metadata.domain.collector.model.CollectSchedule;
import com.yss.metadata.domain.collector.model.CollectorMode;
import com.yss.metadata.domain.collector.model.CollectorStrategy;
import com.yss.metadata.domain.collector.model.CollectorTask;
import com.yss.metadata.domain.collector.model.CollectorTaskStatus;
import com.yss.metadata.domain.collector.gateway.CollectorTaskGateway;
import com.yss.metadata.infrastructure.convertor.CollectorTaskConvertor;
import com.yss.metadata.repository.gateway.impl.CollectorTaskGatewayImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 采集任务仓储持久化集成测试（WU-01-03，H2 内存库替代真实 MySQL）。
 *
 * <p>验证 PO/Mapper（yss BaseRepository + EntityProvider 注解 SQL）到
 * Domain 端口的完整读写链路，含同源+调度唯一性与状态/失败原因持久化。</p>
 */
class CollectorTaskGatewayImplH2Test extends H2MapperTestSupport {

    private CollectorTaskGateway repository;

    @BeforeEach
    void setUp() {
        repository = new CollectorTaskGatewayImpl(sqlSession.getMapper(CollectorTaskRepository.class),
                Mappers.getMapper(CollectorTaskConvertor.class));
    }

    @Test
    @DisplayName("新增采集任务并回读：字段完整往返")
    void saveAndFindByIdRoundTrip() {
        CollectorTask task = buildTask("ct-1", "c-1", "0 0 2 * * ?");

        repository.save(task);

        Optional<CollectorTask> loaded = repository.findById("ct-1");
        assertThat(loaded).isPresent();
        CollectorTask actual = loaded.orElseThrow(AssertionError::new);
        assertThat(actual.getName()).isEqualTo("每日元数据采集");
        assertThat(actual.getConnectorId()).isEqualTo("c-1");
        assertThat(actual.getSchedule()).isEqualTo(new CollectSchedule("0 0 2 * * ?"));
        assertThat(actual.getMode()).isEqualTo(CollectorMode.INCREMENTAL);
        assertThat(actual.getStrategy()).isEqualTo(CollectorStrategy.IGNORE);
        assertThat(actual.getAutoClassify()).isTrue();
        assertThat(actual.getStatus()).isEqualTo(CollectorTaskStatus.PENDING);
        assertThat(actual.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("状态流转与失败原因持久化：运行中→失败并回读失败原因")
    void saveStateAndFailReasonPersists() {
        repository.save(buildTask("ct-1", "c-1", "0 0 2 * * ?"));

        CollectorTask task = repository.findById("ct-1").orElseThrow(AssertionError::new);
        task.start();
        task.markFailed("连接超时：table scan failed");
        repository.save(task);

        CollectorTask loaded = repository.findById("ct-1").orElseThrow(AssertionError::new);
        assertThat(loaded.getStatus()).isEqualTo(CollectorTaskStatus.FAILED);
        assertThat(loaded.getFailReason()).isEqualTo("连接超时：table scan failed");
        assertThat(loaded.getLastRunAt()).isNotNull();
    }

    @Test
    @DisplayName("同数据源+调度唯一性检查：存在与排除自身")
    void existsByConnectorAndScheduleChecks() {
        repository.save(buildTask("ct-1", "c-1", "0 0 2 * * ?"));
        repository.save(buildTask("ct-2", "c-1", "0 0 3 * * ?"));

        assertThat(repository.existsByConnectorIdAndSchedule("c-1", new CollectSchedule("0 0 2 * * ?")))
                .isTrue();
        assertThat(repository.existsByConnectorIdAndSchedule("c-1", new CollectSchedule("0 0 4 * * ?")))
                .isFalse();
        assertThat(repository.existsByConnectorIdAndScheduleExcluding("c-1", new CollectSchedule("0 0 2 * * ?"), "ct-1"))
                .isFalse();
        assertThat(repository.existsByConnectorIdAndScheduleExcluding("c-1", new CollectSchedule("0 0 2 * * ?"), "other"))
                .isTrue();
    }

    @Test
    @DisplayName("列表返回全部采集任务，删除后移除")
    void findAllAndDelete() {
        repository.save(buildTask("ct-1", "c-1", "0 0 2 * * ?"));
        repository.save(buildTask("ct-2", "c-2", "0 0 3 * * ?"));

        List<CollectorTask> all = repository.findAll();
        assertThat(all).hasSize(2);

        repository.deleteById("ct-1");
        assertThat(repository.findById("ct-1")).isEmpty();
        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("按关键词与生效状态条件查询：正确过滤并返回")
    void findByQueryFiltering() {
        CollectorTask task1 = buildTask("ct-1", "c-1", "0 0 2 * * ?");
        task1.setName("营销域增量采集");
        task1.setOwner("1397905662202719");
        task1.setEnabled(Boolean.TRUE);
        task1.setDatasourceType("MySQL");
        repository.save(task1);

        CollectorTask task2 = buildTask("ct-2", "c-2", "0 0 3 * * ?");
        task2.setName("风控全量采集");
        task2.setOwner("data_eng");
        task2.setEnabled(Boolean.FALSE);
        task2.setDatasourceType("Oracle");
        repository.save(task2);

        com.yss.metadata.client.dto.query.CollectorQuery query = com.yss.metadata.client.dto.query.CollectorQuery.builder()
                .keyword("营销")
                .enabled(Boolean.TRUE)
                .build();
        List<CollectorTask> results = repository.findByQuery(query);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("营销域增量采集");
    }

    private CollectorTask buildTask(String id, String connectorId, String schedule) {
        return CollectorTask.builder()
                .id(id)
                .name("每日元数据采集")
                .connectorId(connectorId)
                .schedule(new CollectSchedule(schedule))
                .mode(CollectorMode.INCREMENTAL)
                .strategy(CollectorStrategy.IGNORE)
                .autoClassify(Boolean.TRUE)
                .status(CollectorTaskStatus.PENDING)
                .createdAt(LocalDateTime.of(2026, 8, 1, 0, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 8, 1, 0, 0, 0))
                .build();
    }
}
