package com.yss.metadata.application.collector.service;

import com.yss.metadata.application.collector.service.convertor.CollectorAppConvertor;
import com.yss.metadata.application.collector.service.impl.CollectorTaskAppServiceImpl;
import com.yss.metadata.application.collector.support.FakeCollectorExecutionSpi;
import com.yss.metadata.application.collector.support.InMemoryAssetGateway;
import com.yss.metadata.application.collector.support.InMemoryCollectorTaskRepository;
import com.yss.metadata.application.connector.service.ConnectorAppService;
import com.yss.metadata.application.connector.service.convertor.ConnectorAppConvertor;
import com.yss.metadata.application.connector.service.impl.ConnectorAppServiceImpl;
import com.yss.metadata.application.connector.support.FakeConnectorTestSpi;
import com.yss.metadata.application.connector.support.InMemoryConnectorRepository;
import com.yss.metadata.application.connector.support.TestCredentialCipher;
import com.yss.metadata.application.governance.service.support.SensitiveRecognitionApplier;
import com.yss.metadata.application.governance.support.InMemoryClassRuleGateway;
import com.yss.metadata.application.governance.support.InMemoryClassificationGateway;
import com.yss.metadata.client.dto.cmd.CollectorAddCmd;
import com.yss.metadata.client.dto.cmd.ConnectorAddCmd;
import com.yss.metadata.client.vo.CollectorVO;
import com.yss.metadata.client.vo.ConnectorVO;
import com.yss.metadata.domain.collector.model.CollectedAsset;
import com.yss.metadata.domain.collector.model.CollectedColumn;
import com.yss.metadata.domain.collector.model.CollectorExecutionResult;
import com.yss.metadata.domain.collector.model.CollectorMode;
import com.yss.metadata.domain.collector.model.CollectorStrategy;
import com.yss.metadata.domain.collector.model.CollectorTaskStatus;
import com.yss.metadata.domain.collector.exception.CollectorTaskNotFoundException;
import com.yss.metadata.domain.collector.exception.CollectorTaskStateConflictException;
import com.yss.metadata.domain.connector.model.ConnectErrorType;
import com.yss.metadata.domain.connector.model.ConnectTestResult;
import com.yss.metadata.domain.connector.exception.ConnectorNotFoundException;
import com.yss.metadata.domain.governance.model.ClassificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 采集编排用例测试（WU-01-03）：start → 连接校验 → 采集执行 → 成功/失败落状态。
 *
 * <p>覆盖：连接校验分类结果衔接失败原因（network/credential/dialect）、
 * 采集执行成功/失败、运行中幂等拒绝（409）、任务/连接器不存在 404、失败重试。</p>
 */
class CollectorOrchestratorTest {

    private InMemoryConnectorRepository connectorRepository;
    private InMemoryCollectorTaskRepository taskRepository;
    private FakeConnectorTestSpi connectorTestSpi;
    private FakeCollectorExecutionSpi collectorExecutionSpi;
    private InMemoryAssetGateway assetGateway;
    private InMemoryClassificationGateway classificationGateway;
    private ConnectorAppService connectorAppService;
    private CollectorTaskAppService collectorTaskAppService;
    private CollectorOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        connectorRepository = new InMemoryConnectorRepository();
        taskRepository = new InMemoryCollectorTaskRepository();
        connectorTestSpi = new FakeConnectorTestSpi();
        collectorExecutionSpi = new FakeCollectorExecutionSpi();
        assetGateway = new InMemoryAssetGateway();
        classificationGateway = new InMemoryClassificationGateway();
        SensitiveRecognitionApplier sensitiveRecognitionApplier =
                new SensitiveRecognitionApplier(new InMemoryClassRuleGateway(), classificationGateway);
        connectorAppService = new ConnectorAppServiceImpl(connectorRepository, taskRepository, connectorTestSpi,
                new TestCredentialCipher(), org.mapstruct.factory.Mappers.getMapper(ConnectorAppConvertor.class));
        CollectorAppConvertor collectorConvertor = org.mapstruct.factory.Mappers.getMapper(CollectorAppConvertor.class);
        collectorTaskAppService = new CollectorTaskAppServiceImpl(taskRepository, collectorConvertor);
        orchestrator = new CollectorOrchestrator(taskRepository, connectorRepository, connectorTestSpi,
                collectorExecutionSpi, assetGateway, sensitiveRecognitionApplier, collectorConvertor);
    }

    @Test
    @DisplayName("编排成功：start → 连接校验通过 → 采集成功 → 任务成功并持久化")
    void runSuccessThenTaskSucceeded() {
        ConnectorVO connector = createConnector("订单库", "10.0.0.1", 3306);
        CollectorVO task = createTask("每日采集", connector.getId(), "0 0 2 * * ?");
        connectorTestSpi.enqueue(ConnectTestResult.success("连接成功"));
        collectorExecutionSpi.enqueue(CollectorExecutionResult.success());

        CollectorVO result = orchestrator.run(task.getId());

        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getFailReason()).isNull();
        assertThat(taskRepository.findById(task.getId()).orElseThrow(AssertionError::new).getStatus())
                .isEqualTo(CollectorTaskStatus.SUCCESS);
    }

    @Test
    @DisplayName("运行中再次触发编排被拒绝（幂等，409 语义）")
    void runWhileRunningRejected() {
        ConnectorVO connector = createConnector("订单库", "10.0.0.1", 3306);
        CollectorVO task = createTask("每日采集", connector.getId(), "0 0 2 * * ?");
        collectorTaskAppService.start(task.getId());

        assertThatThrownBy(() -> orchestrator.run(task.getId()))
                .isInstanceOf(CollectorTaskStateConflictException.class)
                .hasMessageContaining("不可重复触发");
    }

    @Test
    @DisplayName("连接校验网络失败：任务标记失败并携带分类失败原因")
    void runWithNetworkFailureMarksFailed() {
        ConnectorVO connector = createConnector("订单库", "10.0.0.1", 3306);
        CollectorVO task = createTask("每日采集", connector.getId(), "0 0 2 * * ?");
        connectorTestSpi.enqueue(ConnectTestResult.failure(ConnectErrorType.NETWORK, "无法连接到主机，请检查网络"));

        CollectorVO result = orchestrator.run(task.getId());

        assertThat(result.getStatus()).isEqualTo("failed");
        assertThat(result.getFailReason()).contains("无法连接到主机");
    }

    @Test
    @DisplayName("连接校验凭据失败：任务标记失败并携带分类失败原因")
    void runWithCredentialFailureMarksFailed() {
        ConnectorVO connector = createConnector("订单库", "10.0.0.1", 3306);
        CollectorVO task = createTask("每日采集", connector.getId(), "0 0 2 * * ?");
        connectorTestSpi.enqueue(ConnectTestResult.failure(ConnectErrorType.CREDENTIAL, "用户名或密码不正确"));

        CollectorVO result = orchestrator.run(task.getId());

        assertThat(result.getStatus()).isEqualTo("failed");
        assertThat(result.getFailReason()).contains("用户名或密码不正确");
    }

    @Test
    @DisplayName("采集执行失败：任务标记失败并持久化失败原因")
    void runWithExecutionFailureMarksFailed() {
        ConnectorVO connector = createConnector("订单库", "10.0.0.1", 3306);
        CollectorVO task = createTask("每日采集", connector.getId(), "0 0 2 * * ?");
        connectorTestSpi.enqueue(ConnectTestResult.success("连接成功"));
        collectorExecutionSpi.enqueue(CollectorExecutionResult.failure("table scan failed"));

        CollectorVO result = orchestrator.run(task.getId());

        assertThat(result.getStatus()).isEqualTo("failed");
        assertThat(result.getFailReason()).isEqualTo("table scan failed");
        assertThat(taskRepository.findById(task.getId()).orElseThrow(AssertionError::new).getFailReason())
                .isEqualTo("table scan failed");
    }

    @Test
    @DisplayName("目标连接器不存在抛未找到（404 语义）")
    void runWithMissingConnectorThrows() {
        CollectorVO task = createTask("每日采集", "not-exist", "0 0 2 * * ?");

        assertThatThrownBy(() -> orchestrator.run(task.getId()))
                .isInstanceOf(ConnectorNotFoundException.class);
    }

    @Test
    @DisplayName("采集任务不存在抛未找到（404 语义）")
    void runWithMissingTaskThrows() {
        assertThatThrownBy(() -> orchestrator.run("not-exist"))
                .isInstanceOf(CollectorTaskNotFoundException.class);
    }

    @Test
    @DisplayName("失败重试：失败任务重新执行成功")
    void retryAfterFailureSucceeds() {
        ConnectorVO connector = createConnector("订单库", "10.0.0.1", 3306);
        CollectorVO task = createTask("每日采集", connector.getId(), "0 0 2 * * ?");
        collectorTaskAppService.start(task.getId());
        collectorTaskAppService.markFailed(task.getId(), "连接超时");
        connectorTestSpi.enqueue(ConnectTestResult.success("连接成功"));
        collectorExecutionSpi.enqueue(CollectorExecutionResult.success());

        CollectorVO result = orchestrator.retry(task.getId(), true);

        assertThat(result.getStatus()).isEqualTo("success");
    }

    @Test
    @DisplayName("重试运行中任务被拒绝（幂等，409 语义）")
    void retryWhileRunningRejected() {
        ConnectorVO connector = createConnector("订单库", "10.0.0.1", 3306);
        CollectorVO task = createTask("每日采集", connector.getId(), "0 0 2 * * ?");
        collectorTaskAppService.start(task.getId());

        assertThatThrownBy(() -> orchestrator.retry(task.getId(), true))
                .isInstanceOf(CollectorTaskStateConflictException.class);
    }

    @Test
    @DisplayName("采集成功携带资产清单：资产经 AssetGateway 入库，任务标记成功")
    void runSuccessPersistsAssets() {
        ConnectorVO connector = createConnector("订单库", "10.0.0.1", 3306);
        CollectorVO task = createTask("每日采集", connector.getId(), "0 0 2 * * ?");
        connectorTestSpi.enqueue(ConnectTestResult.success("连接成功"));
        collectorExecutionSpi.enqueue(CollectorExecutionResult.success(collectedAssets("orders")));

        CollectorVO result = orchestrator.run(task.getId());

        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(assetGateway.getSaved()).hasSize(1);
        InMemoryAssetGateway.SavedBatch batch = assetGateway.getSaved().get(0);
        assertThat(batch.getSourceId()).isEqualTo(connector.getId());
        assertThat(batch.getAssets()).hasSize(1);
        assertThat(batch.getAssets().get(0).getName()).isEqualTo("orders");
        assertThat(taskRepository.findById(task.getId()).orElseThrow(AssertionError::new).getStatus())
                .isEqualTo(CollectorTaskStatus.SUCCESS);
    }

    @Test
    @DisplayName("采集成功但无资产清单：不触发资产入库调用")
    void runSuccessWithoutAssetsSkipsSave() {
        ConnectorVO connector = createConnector("订单库", "10.0.0.1", 3306);
        CollectorVO task = createTask("每日采集", connector.getId(), "0 0 2 * * ?");
        connectorTestSpi.enqueue(ConnectTestResult.success("连接成功"));
        collectorExecutionSpi.enqueue(CollectorExecutionResult.success());

        CollectorVO result = orchestrator.run(task.getId());

        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(assetGateway.getSaved()).isEmpty();
    }

    @Test
    @DisplayName("资产入库失败：任务标记失败并携带入库失败原因")
    void runWithAssetSaveFailureMarksFailed() {
        ConnectorVO connector = createConnector("订单库", "10.0.0.1", 3306);
        CollectorVO task = createTask("每日采集", connector.getId(), "0 0 2 * * ?");
        connectorTestSpi.enqueue(ConnectTestResult.success("连接成功"));
        collectorExecutionSpi.enqueue(CollectorExecutionResult.success(collectedAssets("orders")));
        assetGateway.setFailure(new IllegalStateException("asset insert failed"));

        CollectorVO result = orchestrator.run(task.getId());

        assertThat(result.getStatus()).isEqualTo("failed");
        assertThat(result.getFailReason()).contains("资产入库失败");
        assertThat(taskRepository.findById(task.getId()).orElseThrow(AssertionError::new).getFailReason())
                .contains("资产入库失败");
    }

    @Test
    @DisplayName("autoClassify：采集入库后对已入库资产运行敏感识别，产出待确认候选")
    void runWithAutoClassifyCreatesPendingCandidates() {
        ConnectorVO connector = createConnector("订单库", "10.0.0.1", 3306);
        CollectorVO task = createTask("每日采集", connector.getId(), "0 0 2 * * ?");
        connectorTestSpi.enqueue(ConnectTestResult.success("连接成功"));
        collectorExecutionSpi.enqueue(CollectorExecutionResult.success(collectedSensitiveAssets("orders")));

        CollectorVO result = orchestrator.run(task.getId());

        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(classificationGateway.store()).hasSize(1);
        com.yss.metadata.domain.governance.model.Classification candidate =
                classificationGateway.store().values().iterator().next();
        assertThat(candidate.getAssetId()).isEqualTo("asset-orders");
        assertThat(candidate.getName()).isEqualTo("敏感-PII");
        assertThat(candidate.getStatus()).isEqualTo(ClassificationStatus.PENDING);
    }

    @Test
    @DisplayName("敏感识别失败（尽力而为）：不使采集任务失败，任务仍标记成功（F7/F10 回归）")
    void runWithRecognitionFailureKeepsTaskSucceeded() {
        ConnectorVO connector = createConnector("订单库", "10.0.0.1", 3306);
        CollectorVO task = createTask("每日采集", connector.getId(), "0 0 2 * * ?");
        connectorTestSpi.enqueue(ConnectTestResult.success("连接成功"));
        collectorExecutionSpi.enqueue(CollectorExecutionResult.success(collectedSensitiveAssets("orders")));

        SensitiveRecognitionApplier throwingApplier = new SensitiveRecognitionApplier(
                new InMemoryClassRuleGateway(), new ThrowingClassificationGateway());
        orchestrator = new CollectorOrchestrator(taskRepository, connectorRepository, connectorTestSpi,
                collectorExecutionSpi, assetGateway, throwingApplier,
                org.mapstruct.factory.Mappers.getMapper(CollectorAppConvertor.class));

        CollectorVO result = orchestrator.run(task.getId());

        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getFailReason()).isNull();
        assertThat(classificationGateway.store()).isEmpty();
    }

    private java.util.List<CollectedAsset> collectedSensitiveAssets(String name) {
        CollectedColumn column = CollectedColumn.builder()
                .name("mobile_no").type("varchar").comment("客户手机号").pk(Boolean.FALSE).build();
        return java.util.Collections.singletonList(CollectedAsset.builder()
                .name(name).type("table").columns(java.util.Collections.singletonList(column)).build());
    }

    private java.util.List<CollectedAsset> collectedAssets(String name) {
        CollectedColumn column = CollectedColumn.builder()
                .name("id").type("bigint").comment("主键").pk(Boolean.TRUE).build();
        return java.util.Collections.singletonList(CollectedAsset.builder()
                .name(name).type("table").columns(java.util.Collections.singletonList(column)).build());
    }

    private ConnectorVO createConnector(String name, String host, int port) {
        ConnectorAddCmd cmd = new ConnectorAddCmd();
        cmd.setName(name);
        cmd.setType(com.yss.metadata.domain.connector.model.ConnectorType.MYSQL);
        cmd.setHost(host);
        cmd.setPort(port);
        cmd.setDialect(com.yss.metadata.domain.connector.model.Dialect.NATIVE);
        cmd.setUsername("root");
        cmd.setPassword("pwd");
        cmd.setAutoClassify(Boolean.TRUE);
        return connectorAppService.create(cmd);
    }

    private CollectorVO createTask(String name, String connectorId, String schedule) {
        CollectorAddCmd cmd = new CollectorAddCmd();
        cmd.setName(name);
        cmd.setConnectorId(connectorId);
        cmd.setSchedule(schedule);
        cmd.setMode(CollectorMode.INCREMENTAL);
        cmd.setStrategy(CollectorStrategy.IGNORE);
        cmd.setAutoClassify(Boolean.TRUE);
        return collectorTaskAppService.create(cmd);
    }

    /** 识别落库抛错的候选网关替身（验证识别失败不使采集任务失败）。 */
    private static final class ThrowingClassificationGateway
            implements com.yss.metadata.domain.governance.gateway.ClassificationGateway {
        @Override
        public java.util.List<com.yss.metadata.domain.governance.model.Classification> findAll() {
            return java.util.Collections.emptyList();
        }

        @Override
        public java.util.Optional<com.yss.metadata.domain.governance.model.Classification> findById(String id) {
            return java.util.Optional.empty();
        }

        @Override
        public com.yss.metadata.domain.governance.model.Classification save(
                com.yss.metadata.domain.governance.model.Classification classification) {
            return classification;
        }

        @Override
        public boolean saveCandidate(com.yss.metadata.domain.governance.model.Classification candidate) {
            throw new IllegalStateException("candidate insert failed");
        }

        @Override
        public java.util.Optional<String> resolveSourceAssetId(
                com.yss.metadata.domain.governance.model.Classification classification) {
            return java.util.Optional.empty();
        }
    }
}
