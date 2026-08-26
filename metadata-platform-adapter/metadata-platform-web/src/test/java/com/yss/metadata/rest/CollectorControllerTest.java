package com.yss.metadata.rest;

import com.yss.metadata.application.collector.service.CollectorOrchestrator;
import com.yss.metadata.application.collector.service.CollectorTaskAppService;
import com.yss.metadata.application.collector.service.convertor.CollectorAppConvertor;
import com.yss.metadata.application.collector.service.impl.CollectorTaskAppServiceImpl;
import com.yss.metadata.application.connector.service.ConnectorAppService;
import com.yss.metadata.application.connector.service.convertor.ConnectorAppConvertor;
import com.yss.metadata.application.connector.service.impl.ConnectorAppServiceImpl;
import com.yss.metadata.application.governance.service.support.SensitiveRecognitionApplier;
import com.yss.metadata.client.vo.CollectorVO;
import com.yss.metadata.client.vo.ConnectorVO;
import com.yss.metadata.domain.collector.model.CollectorExecutionResult;
import com.yss.metadata.domain.connector.model.ConnectErrorType;
import com.yss.metadata.domain.connector.model.Connector;
import com.yss.metadata.domain.connector.gateway.ConnectorGateway;
import com.yss.metadata.domain.connector.model.ConnectTestResult;
import com.yss.metadata.rest.advice.MetadataGlobalExceptionHandler;
import com.yss.metadata.rest.support.FakeCollectorExecutionSpi;
import com.yss.metadata.rest.support.FakeConnectorTestSpi;
import com.yss.metadata.rest.support.InMemoryAssetGateway;
import com.yss.metadata.rest.support.InMemoryClassRuleGateway;
import com.yss.metadata.rest.support.InMemoryClassificationGateway;
import com.yss.metadata.rest.support.InMemoryCollectorTaskRepository;
import com.yss.metadata.rest.support.TestCredentialCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 采集任务 REST 契约测试（WU-01-03，冻结 OpenAPI collectors 段）。
 *
 * <p>覆盖：GET/POST /api/collectors、PUT /api/collectors/{id}、
 * POST /api/collectors/run、/{id}/cancel、/{id}/retry；
 * 200/201/202/404/409/422 状态与 YSS Result 包装 / Error 错误结构。</p>
 */
class CollectorControllerTest {

    private static final String COLLECTORS_PATH = "/api/collectors";

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private ConnectorGateway connectorRepository;
    private InMemoryCollectorTaskRepository taskRepository;
    private FakeConnectorTestSpi connectorTestSpi;
    private FakeCollectorExecutionSpi collectorExecutionSpi;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        connectorRepository = createMockConnectorGateway();
        taskRepository = new InMemoryCollectorTaskRepository();
        connectorTestSpi = new FakeConnectorTestSpi();
        collectorExecutionSpi = new FakeCollectorExecutionSpi();

        ConnectorAppConvertor connectorConvertor = org.mapstruct.factory.Mappers.getMapper(ConnectorAppConvertor.class);
        CollectorAppConvertor collectorConvertor = org.mapstruct.factory.Mappers.getMapper(CollectorAppConvertor.class);
        ConnectorAppService connectorAppService = new ConnectorAppServiceImpl(connectorRepository, taskRepository,
                connectorTestSpi, new TestCredentialCipher(), connectorConvertor);
        CollectorTaskAppService collectorTaskAppService =
                new CollectorTaskAppServiceImpl(taskRepository, collectorConvertor);
        CollectorOrchestrator orchestrator = new CollectorOrchestrator(taskRepository, connectorRepository,
                connectorTestSpi, collectorExecutionSpi, new InMemoryAssetGateway(),
                new SensitiveRecognitionApplier(new InMemoryClassRuleGateway(), new InMemoryClassificationGateway()),
                collectorConvertor);

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ConnectorController(connectorAppService),
                        new CollectorController(collectorTaskAppService, orchestrator))
                .setControllerAdvice(new MetadataGlobalExceptionHandler())
                .build();
    }

    private static ConnectorGateway createMockConnectorGateway() {
        ConnectorGateway mock = org.mockito.Mockito.mock(ConnectorGateway.class);
        java.util.concurrent.ConcurrentHashMap<String, Connector> store = new java.util.concurrent.ConcurrentHashMap<>();
        org.mockito.Mockito.when(mock.findAll()).thenAnswer(inv -> new java.util.ArrayList<>(store.values()));
        org.mockito.Mockito.when(mock.findById(org.mockito.ArgumentMatchers.anyString())).thenAnswer(inv -> java.util.Optional.ofNullable(store.get(inv.getArgument(0))));
        org.mockito.Mockito.when(mock.save(org.mockito.ArgumentMatchers.any(Connector.class))).thenAnswer(inv -> {
            Connector c = inv.getArgument(0);
            store.put(c.getId(), c);
            return c;
        });
        org.mockito.Mockito.when(mock.existsByName(org.mockito.ArgumentMatchers.anyString())).thenAnswer(inv -> {
            String name = inv.getArgument(0);
            return store.values().stream().anyMatch(c -> name.equals(c.getName()));
        });
        org.mockito.Mockito.when(mock.existsByNameExcluding(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString())).thenAnswer(inv -> {
            String name = inv.getArgument(0);
            String excludeId = inv.getArgument(1);
            return store.values().stream().anyMatch(c -> !c.getId().equals(excludeId) && name.equals(c.getName()));
        });
        org.mockito.Mockito.doAnswer(inv -> {
            store.remove(inv.getArgument(0));
            return null;
        }).when(mock).deleteById(org.mockito.ArgumentMatchers.anyString());
        org.mockito.Mockito.when(mock.getSystemCatalog()).thenReturn(java.util.Collections.singletonList(
                com.yss.metadata.client.vo.DataSourceSystemVO.builder().code("Trading-Core").name("核心交易系统").label("核心交易系统 (Trading-Core)").build()
        ));
        org.mockito.Mockito.when(mock.listDatabases(org.mockito.ArgumentMatchers.anyString())).thenReturn(java.util.Collections.singletonList("db_trade_core"));
        return mock;
    }

    // ---------- GET /api/collectors ----------

    @Test
    @DisplayName("列表返回 200 与 YSS Result 包装字段")
    void listReturns200WithWrapper() throws Exception {
        createTask("任务一", "c-1", "0 0 2 * * ?");

        mockMvc.perform(get(COLLECTORS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("DM-A0001"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("任务一"))
                .andExpect(jsonPath("$.data[0].status").value("pending"));
    }

    @Test
    @DisplayName("按关键词与生效状态条件过滤返回匹配的任务")
    void listWithQueryFilteringReturnsFilteredResults() throws Exception {
        createTask("营销域增量采集", "crm-ds", "0 0 2 * * ?");
        createTask("风控全量采集", "risk-ds", "0 0 4 * * ?");

        mockMvc.perform(get(COLLECTORS_PATH)
                        .param("keyword", "营销")
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("营销域增量采集"));
    }

    @Test
    @DisplayName("切换生效状态返回 200 且 enabled 字段更新")
    void toggleStatusReturns200() throws Exception {
        CollectorVO created = createTask("任务一", "c-1", "0 0 2 * * ?");

        mockMvc.perform(put(COLLECTORS_PATH + "/" + created.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(put(COLLECTORS_PATH + "/" + created.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    // ---------- POST /api/collectors ----------

    @Test
    @DisplayName("创建采集任务返回 201 与待执行数据")
    void createReturns201() throws Exception {
        mockMvc.perform(post(COLLECTORS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"每日采集\",\"connectorId\":\"c-1\",\"schedule\":\"0 0 2 * * ?\","
                                + "\"mode\":\"incremental\",\"strategy\":\"ignore\",\"autoClassify\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("每日采集"))
                .andExpect(jsonPath("$.data.connectorId").value("c-1"))
                .andExpect(jsonPath("$.data.schedule").value("0 0 2 * * ?"))
                .andExpect(jsonPath("$.data.mode").value("incremental"))
                .andExpect(jsonPath("$.data.strategy").value("ignore"))
                .andExpect(jsonPath("$.data.autoClassify").value(true))
                .andExpect(jsonPath("$.data.status").value("pending"));
    }

    @Test
    @DisplayName("创建同数据源+同调度任务返回 409 错误体（collector.conflict）")
    void createDuplicateReturns409() throws Exception {
        createTask("任务一", "c-1", "0 0 2 * * ?");

        mockMvc.perform(post(COLLECTORS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"任务二\",\"connectorId\":\"c-1\",\"schedule\":\"0 0 2 * * ?\","
                                + "\"mode\":\"full\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("collector.conflict"))
                .andExpect(jsonPath("$.severity").value("error"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    @Test
    @DisplayName("参数校验失败返回 422 且 fieldErrors 含字段级错误")
    void createValidationErrorReturns422() throws Exception {
        mockMvc.perform(post(COLLECTORS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"connectorId\":\"c-1\",\"schedule\":\"0 0 2 * * ?\",\"mode\":\"incremental\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("param.invalid"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    // ---------- PUT /api/collectors/{id} ----------

    @Test
    @DisplayName("编辑调度返回 200 与更新后数据（状态重置待执行）")
    void updateReturns200() throws Exception {
        CollectorVO created = createTask("任务一", "c-1", "0 0 2 * * ?");

        mockMvc.perform(put(COLLECTORS_PATH + "/" + created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + created.getId() + "\",\"name\":\"任务一\",\"connectorId\":\"c-1\","
                                + "\"schedule\":\"0 0 4 * * ?\",\"mode\":\"full\",\"strategy\":\"overwrite\","
                                + "\"autoClassify\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.schedule").value("0 0 4 * * ?"))
                .andExpect(jsonPath("$.data.mode").value("full"))
                .andExpect(jsonPath("$.data.strategy").value("overwrite"))
                .andExpect(jsonPath("$.data.status").value("pending"));
    }

    @Test
    @DisplayName("编辑不存在的采集任务返回 404")
    void updateNotFoundReturns404() throws Exception {
        mockMvc.perform(put(COLLECTORS_PATH + "/not-exist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"not-exist\",\"name\":\"任务\",\"connectorId\":\"c-1\","
                                + "\"schedule\":\"0 0 2 * * ?\",\"mode\":\"incremental\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("collector.not_found"));
    }

    // ---------- POST /api/collectors/run ----------

    @Test
    @DisplayName("立即执行返回 202，采集成功任务流转成功")
    void runReturns202AndSucceeds() throws Exception {
        ConnectorVO connector = createConnector("订单库");
        CollectorVO task = createTask("任务一", connector.getId(), "0 0 2 * * ?");
        connectorTestSpi.enqueue(ConnectTestResult.success("连接成功"));
        collectorExecutionSpi.enqueue(CollectorExecutionResult.success());

        mockMvc.perform(post(COLLECTORS_PATH + "/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"collectorId\":\"" + task.getId() + "\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("success"));
    }

    @Test
    @DisplayName("运行中再次触发返回 409（幂等拒绝，collector.state_conflict）")
    void runWhileRunningReturns409() throws Exception {
        CollectorVO task = createTask("任务一", "c-1", "0 0 2 * * ?");
        // 直接置为运行中（绕过编排），随后触发 run 应幂等拒绝
        taskRepository.findById(task.getId()).orElseThrow(AssertionError::new).start();
        taskRepository.save(taskRepository.findById(task.getId()).orElseThrow(AssertionError::new));

        mockMvc.perform(post(COLLECTORS_PATH + "/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"collectorId\":\"" + task.getId() + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("collector.state_conflict"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("连接校验失败返回 202，任务标记失败并携带分类原因")
    void runWithNetworkFailureReturns202Failed() throws Exception {
        ConnectorVO connector = createConnector("订单库");
        CollectorVO task = createTask("任务一", connector.getId(), "0 0 2 * * ?");
        connectorTestSpi.enqueue(ConnectTestResult.failure(ConnectErrorType.NETWORK, "无法连接到主机，请检查网络"));

        mockMvc.perform(post(COLLECTORS_PATH + "/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"collectorId\":\"" + task.getId() + "\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("failed"))
                .andExpect(jsonPath("$.data.failReason").value("无法连接到主机，请检查网络"));
    }

    @Test
    @DisplayName("执行不存在的任务返回 404")
    void runNotFoundReturns404() throws Exception {
        mockMvc.perform(post(COLLECTORS_PATH + "/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"collectorId\":\"not-exist\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("collector.not_found"));
    }

    // ---------- POST /api/collectors/{id}/cancel ----------

    @Test
    @DisplayName("取消运行中任务返回 200，状态已取消")
    void cancelRunningReturns200() throws Exception {
        CollectorVO task = createTask("任务一", "c-1", "0 0 2 * * ?");
        taskRepository.findById(task.getId()).orElseThrow(AssertionError::new).start();
        taskRepository.save(taskRepository.findById(task.getId()).orElseThrow(AssertionError::new));

        mockMvc.perform(post(COLLECTORS_PATH + "/" + task.getId() + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("cancelled"));
    }

    @Test
    @DisplayName("取消待执行任务返回 409（取消仅运行中）")
    void cancelPendingReturns409() throws Exception {
        CollectorVO task = createTask("任务一", "c-1", "0 0 2 * * ?");

        mockMvc.perform(post(COLLECTORS_PATH + "/" + task.getId() + "/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("collector.state_conflict"));
    }

    // ---------- POST /api/collectors/{id}/retry ----------

    @Test
    @DisplayName("失败任务重试返回 202，重新执行成功")
    void retryFailedTaskReturns202() throws Exception {
        ConnectorVO connector = createConnector("订单库");
        CollectorVO task = createTask("任务一", connector.getId(), "0 0 2 * * ?");
        taskRepository.findById(task.getId()).orElseThrow(AssertionError::new).start();
        taskRepository.save(taskRepository.findById(task.getId()).orElseThrow(AssertionError::new));
        taskRepository.findById(task.getId()).orElseThrow(AssertionError::new).markFailed("连接超时");
        taskRepository.save(taskRepository.findById(task.getId()).orElseThrow(AssertionError::new));
        connectorTestSpi.enqueue(ConnectTestResult.success("连接成功"));
        collectorExecutionSpi.enqueue(CollectorExecutionResult.success());

        mockMvc.perform(post(COLLECTORS_PATH + "/" + task.getId() + "/retry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"failedItemsOnly\":true}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("success"));
    }

    // ---------- GET /api/collectors/{id} ----------

    @Test
    @DisplayName("根据 ID 获取采集任务详情返回 200 与任务数据")
    void getByIdReturns200WithTaskData() throws Exception {
        CollectorVO task = createTask("详情测试任务", "c-1", "0 0 2 * * ?");

        mockMvc.perform(get(COLLECTORS_PATH + "/" + task.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(task.getId()))
                .andExpect(jsonPath("$.data.name").value("详情测试任务"))
                .andExpect(jsonPath("$.data.status").value("pending"));
    }

    @Test
    @DisplayName("根据 ID 获取不存在的采集任务返回 404")
    void getByIdNotFoundReturns404() throws Exception {
        mockMvc.perform(get(COLLECTORS_PATH + "/non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("collector.not_found"));
    }

    // ---------- DELETE /api/collectors/{id} ----------

    @Test
    @DisplayName("删除未运行采集任务返回 204 并成功移除")
    void deleteTaskReturns204AndRemoves() throws Exception {
        CollectorVO task = createTask("待删除任务", "c-1", "0 0 2 * * ?");

        mockMvc.perform(delete(COLLECTORS_PATH + "/" + task.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(COLLECTORS_PATH + "/" + task.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("collector.not_found"));
    }

    @Test
    @DisplayName("删除运行中的采集任务返回 409 状态冲突")
    void deleteRunningTaskReturns409() throws Exception {
        CollectorVO task = createTask("运行中任务", "c-1", "0 0 2 * * ?");
        taskRepository.findById(task.getId()).orElseThrow(AssertionError::new).start();
        taskRepository.save(taskRepository.findById(task.getId()).orElseThrow(AssertionError::new));

        mockMvc.perform(delete(COLLECTORS_PATH + "/" + task.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("collector.state_conflict"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("运行中的采集任务不能删除")));
    }

    @Test
    @DisplayName("删除不存在的采集任务返回 404")
    void deleteNotFoundReturns404() throws Exception {
        mockMvc.perform(delete(COLLECTORS_PATH + "/non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("collector.not_found"));
    }

    private ConnectorVO createConnector(String name) throws Exception {
        String response = mockMvc.perform(post("/api/connectors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"type\":\"MySQL\",\"host\":\"10.0.0.1\",\"port\":3306,"
                                + "\"dialect\":\"native\",\"username\":\"root\",\"password\":\"pwd\","
                                + "\"autoClassify\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return MAPPER.readValue(MAPPER.readTree(response).path("data").toString(), ConnectorVO.class);
    }

    private CollectorVO createTask(String name, String connectorId, String schedule) throws Exception {
        String response = mockMvc.perform(post(COLLECTORS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"connectorId\":\"" + connectorId + "\","
                                + "\"schedule\":\"" + schedule + "\",\"mode\":\"incremental\","
                                + "\"strategy\":\"ignore\",\"autoClassify\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return MAPPER.readValue(MAPPER.readTree(response).path("data").toString(), CollectorVO.class);
    }
}
