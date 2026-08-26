package com.yss.metadata.repository;

import com.yss.metadata.client.dto.query.CollectorInstanceQuery;
import com.yss.metadata.domain.collector.gateway.CollectorInstanceGateway;
import com.yss.metadata.domain.collector.model.CollectorInstance;
import com.yss.metadata.domain.collector.model.CollectorInstanceStatus;
import com.yss.metadata.domain.collector.model.ExecutionMode;
import com.yss.metadata.domain.collector.model.MetadataDiffSummary;
import com.yss.metadata.domain.collector.model.WorkflowNode;
import com.yss.metadata.domain.collector.model.WorkflowNodeType;
import com.yss.metadata.infrastructure.convertor.CollectorInstanceConvertor;
import com.yss.metadata.repository.gateway.impl.CollectorInstanceGatewayImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 采集实例仓储持久化集成测试（H2 内存库替代真实 MySQL）。
 *
 * <p>验证 PO/Mapper (MyBatis-Plus + JacksonTypeHandler) 到 Domain 端口的完整读写链路，
 * 覆盖标量字段与 JSON 复杂结构（工作流节点列表、Diff 比对摘要）的读写往返、条件查询下推与排序。</p>
 */
class CollectorInstanceGatewayImplH2Test extends H2MapperTestSupport {

    private CollectorInstanceGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new CollectorInstanceGatewayImpl(
                sqlSession.getMapper(CollectorInstanceRepository.class),
                Mappers.getMapper(CollectorInstanceConvertor.class));
    }

    @Test
    @DisplayName("新增采集实例并回读：基础字段与 JSON 复杂结构完整往返")
    void saveAndFindByIdRoundTrip() {
        CollectorInstance instance = buildSampleInstance("inst-1", "col-1", CollectorInstanceStatus.SUCCESS);
        gateway.save(instance);

        Optional<CollectorInstance> loaded = gateway.findById("inst-1");
        assertThat(loaded).isPresent();
        CollectorInstance actual = loaded.orElseThrow(AssertionError::new);
        assertThat(actual.getName()).isEqualTo("MySQL采集任务实例");
        assertThat(actual.getCollectorId()).isEqualTo("col-1");
        assertThat(actual.getCollectorName()).isEqualTo("MySQL采集任务");
        assertThat(actual.getConnectorId()).isEqualTo("conn-1");
        assertThat(actual.getDatasourceType()).isEqualTo("MySQL");
        assertThat(actual.getStatus()).isEqualTo(CollectorInstanceStatus.SUCCESS);
        assertThat(actual.getExecutionMode()).isEqualTo(ExecutionMode.MANUAL);
        assertThat(actual.getDurationMs()).isEqualTo(45000L);
        assertThat(actual.getExecutor()).isEqualTo("admin");
        assertThat(actual.getOwner()).isEqualTo("admin");

        // 验证 workflowNodes JSON 结构反序列化
        assertThat(actual.getWorkflowNodes()).hasSize(2);
        WorkflowNode node1 = actual.getWorkflowNodes().get(0);
        assertThat(node1.getName()).isEqualTo("JDBC 连通性探测");
        assertThat(node1.getType()).isEqualTo(WorkflowNodeType.JDBC_PROBE);
        assertThat(node1.getStatus()).isEqualTo(CollectorInstanceStatus.SUCCESS);
        assertThat(node1.getLogs()).containsExactly("[INFO] 探测成功");

        WorkflowNode node2 = actual.getWorkflowNodes().get(1);
        assertThat(node2.getType()).isEqualTo(WorkflowNodeType.DLINK);
        assertThat(node2.getPerformanceMetrics()).containsEntry("throughput", "10,000 records/sec");

        // 验证 diffSummary JSON 结构反序列化
        assertThat(actual.getDiffSummary()).isNotNull();
        assertThat(actual.getDiffSummary().getTotalObjects()).isEqualTo(100);
        assertThat(actual.getDiffSummary().getTableDetails()).hasSize(1);
        assertThat(actual.getDiffSummary().getTableDetails().get(0).getTableName()).isEqualTo("user_info");
    }

    @Test
    @DisplayName("状态流转与终止：终止运行中实例并持久化状态与耗时")
    void stateTransitionAndTerminationPersists() {
        CollectorInstance instance = buildSampleInstance("inst-running", "col-1", CollectorInstanceStatus.RUNNING);
        gateway.save(instance);

        CollectorInstance loaded = gateway.findById("inst-running").orElseThrow(AssertionError::new);
        loaded.terminate("admin", "手动取消");
        gateway.save(loaded);

        CollectorInstance updated = gateway.findById("inst-running").orElseThrow(AssertionError::new);
        assertThat(updated.getStatus()).isEqualTo(CollectorInstanceStatus.FAILED);
        assertThat(updated.getErrorMessage()).contains("手动取消");
        assertThat(updated.getEndTime()).isNotNull();
    }

    @Test
    @DisplayName("多维度组合条件查询下推：关键字、状态、时间范围过滤正确生效")
    void findByQueryFiltering() {
        CollectorInstance inst1 = buildSampleInstance("inst-1", "col-mysql", CollectorInstanceStatus.SUCCESS);
        inst1.setName("CRM_MySQL抽取");
        inst1.setOwner("user_a");
        inst1.setExecutor("user_a");
        inst1.setDatasourceType("MySQL");
        inst1.setStartTime(LocalDateTime.of(2026, 8, 10, 10, 0, 0));
        gateway.save(inst1);

        CollectorInstance inst2 = buildSampleInstance("inst-2", "col-oracle", CollectorInstanceStatus.FAILED);
        inst2.setName("ERP_Oracle抽取");
        inst2.setOwner("user_b");
        inst2.setExecutor("user_b");
        inst2.setDatasourceType("Oracle");
        inst2.setStartTime(LocalDateTime.of(2026, 8, 12, 10, 0, 0));
        gateway.save(inst2);

        // 1. 关键字搜索
        CollectorInstanceQuery q1 = CollectorInstanceQuery.builder().keyword("CRM").build();
        List<CollectorInstance> r1 = gateway.findByQuery(q1);
        assertThat(r1).hasSize(1);
        assertThat(r1.get(0).getId()).isEqualTo("inst-1");

        // 2. 仅失败
        CollectorInstanceQuery q2 = CollectorInstanceQuery.builder().onlyFailed(Boolean.TRUE).build();
        List<CollectorInstance> r2 = gateway.findByQuery(q2);
        assertThat(r2).hasSize(1);
        assertThat(r2.get(0).getId()).isEqualTo("inst-2");

        // 3. 负责人 + 数据源类型
        CollectorInstanceQuery q3 = CollectorInstanceQuery.builder().owner("user_a").datasourceType("MySQL").build();
        List<CollectorInstance> r3 = gateway.findByQuery(q3);
        assertThat(r3).hasSize(1);
        assertThat(r3.get(0).getId()).isEqualTo("inst-1");

        // 4. 时间范围
        CollectorInstanceQuery q4 = CollectorInstanceQuery.builder()
                .startTimeBegin("2026-08-11T00:00:00")
                .startTimeEnd("2026-08-13T00:00:00")
                .build();
        List<CollectorInstance> r4 = gateway.findByQuery(q4);
        assertThat(r4).hasSize(1);
        assertThat(r4.get(0).getId()).isEqualTo("inst-2");
    }

    @Test
    @DisplayName("按开始时间倒序返回列表与按 ID 删除")
    void findAllAndOrderByStartTimeDescAndDelete() {
        CollectorInstance inst1 = buildSampleInstance("inst-early", "col-1", CollectorInstanceStatus.SUCCESS);
        inst1.setStartTime(LocalDateTime.of(2026, 8, 1, 10, 0, 0));
        gateway.save(inst1);

        CollectorInstance inst2 = buildSampleInstance("inst-later", "col-1", CollectorInstanceStatus.SUCCESS);
        inst2.setStartTime(LocalDateTime.of(2026, 8, 2, 10, 0, 0));
        gateway.save(inst2);

        List<CollectorInstance> all = gateway.findAll();
        assertThat(all).hasSize(2);
        assertThat(all.get(0).getId()).isEqualTo("inst-later");
        assertThat(all.get(1).getId()).isEqualTo("inst-early");

        gateway.deleteById("inst-early");
        assertThat(gateway.findById("inst-early")).isEmpty();
        assertThat(gateway.findAll()).hasSize(1);
    }

    private CollectorInstance buildSampleInstance(String id, String collectorId, CollectorInstanceStatus status) {
        LocalDateTime now = LocalDateTime.now();
        List<WorkflowNode> nodes = new ArrayList<>();
        nodes.add(WorkflowNode.builder()
                .id(id + "-node-1")
                .name("JDBC 连通性探测")
                .type(WorkflowNodeType.JDBC_PROBE)
                .status(CollectorInstanceStatus.SUCCESS)
                .startTime(now.minusMinutes(5))
                .endTime(now.minusMinutes(4))
                .durationMs(60000L)
                .logs(new ArrayList<>(Collections.singletonList("[INFO] 探测成功")))
                .build());

        Map<String, Object> perf = new HashMap<>();
        perf.put("throughput", "10,000 records/sec");
        nodes.add(WorkflowNode.builder()
                .id(id + "-node-2")
                .name("Dlink 分布式元数据抽取")
                .type(WorkflowNodeType.DLINK)
                .status(status)
                .startTime(now.minusMinutes(4))
                .endTime(status == CollectorInstanceStatus.SUCCESS ? now : null)
                .performanceMetrics(perf)
                .build());

        MetadataDiffSummary diff = MetadataDiffSummary.builder()
                .instanceId(id)
                .datasourceName("Dev MySQL")
                .totalObjects(100)
                .totalTables(20)
                .totalViews(5)
                .totalColumns(75)
                .tableDetails(new ArrayList<>(Collections.singletonList(
                        MetadataDiffSummary.TableDiffItem.builder()
                                .tableName("user_info")
                                .diffType("ADDED")
                                .columnCount(10)
                                .rowCount(5000L)
                                .build()
                )))
                .build();

        return CollectorInstance.builder()
                .id(id)
                .name("MySQL采集任务实例")
                .collectorId(collectorId)
                .collectorName("MySQL采集任务")
                .connectorId("conn-1")
                .connectorName("Dev MySQL")
                .datasourceType("MySQL")
                .status(status)
                .executionMode(ExecutionMode.MANUAL)
                .scheduleDescription("手动执行")
                .startTime(now.minusMinutes(5))
                .endTime(status == CollectorInstanceStatus.SUCCESS ? now : null)
                .durationMs(45000L)
                .executor("admin")
                .owner("admin")
                .isDryRun(Boolean.FALSE)
                .retryCount(0)
                .maxRetries(3)
                .workflowNodes(nodes)
                .diffSummary(diff)
                .build();
    }
}
