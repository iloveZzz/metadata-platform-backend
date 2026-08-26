package com.yss.metadata.rest;

import com.yss.metadata.application.connector.service.ConnectorAppService;
import com.yss.metadata.application.connector.service.convertor.ConnectorAppConvertor;
import com.yss.metadata.application.connector.service.impl.ConnectorAppServiceImpl;
import com.yss.metadata.client.vo.ConnectorVO;
import com.yss.metadata.domain.collector.model.CollectSchedule;
import com.yss.metadata.domain.collector.model.CollectorMode;
import com.yss.metadata.domain.collector.model.CollectorStrategy;
import com.yss.metadata.domain.collector.model.CollectorTask;
import com.yss.metadata.domain.collector.model.CollectorTaskStatus;
import com.yss.metadata.domain.connector.model.ConnectErrorType;
import com.yss.metadata.domain.connector.model.ConnectTestResult;
import com.yss.metadata.domain.connector.model.Connector;
import com.yss.metadata.domain.connector.gateway.ConnectorGateway;
import com.yss.metadata.rest.advice.MetadataGlobalExceptionHandler;
import com.yss.metadata.rest.support.FakeConnectorTestSpi;
import com.yss.metadata.rest.support.InMemoryCollectorTaskRepository;
import com.yss.metadata.rest.support.TestCredentialCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 连接器 REST 契约测试（WU-01-01，冻结 OpenAPI connectors 段）。
 *
 * <p>覆盖：GET/POST /api/connectors、PUT/DELETE /api/connectors/{id}、
 * POST /api/connectors/{id}/test；YSS Result 包装字段
 * （success/code/message/tips/dataType）；错误体结构
 * （code/message/severity/fieldErrors）与错误分类（network/credential/dialect）。</p>
 */
class ConnectorControllerTest {

    private static final String CONNECTORS_PATH = "/api/connectors";

    private ConnectorGateway repository;
    private InMemoryCollectorTaskRepository taskRepository;
    private FakeConnectorTestSpi connectorTestSpi;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        repository = createMockConnectorGateway();
        taskRepository = new InMemoryCollectorTaskRepository();
        connectorTestSpi = new FakeConnectorTestSpi();
        ConnectorAppService appService = new ConnectorAppServiceImpl(repository, taskRepository,
                connectorTestSpi, new TestCredentialCipher(), org.mapstruct.factory.Mappers.getMapper(ConnectorAppConvertor.class));
        mockMvc = MockMvcBuilders.standaloneSetup(new ConnectorController(appService))
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

    // ---------- GET /api/connectors ----------

    @Test
    @DisplayName("列表返回 200 与 YSS Result 包装字段（success/code/message/tips/dataType）")
    void listReturns200WithResultWrapper() throws Exception {
        createConnector("{\"name\":\"订单库\",\"type\":\"MySQL\",\"host\":\"10.0.0.1\",\"port\":3306,"
                + "\"dialect\":\"native\",\"username\":\"root\",\"password\":\"pwd\",\"autoClassify\":true}");

        mockMvc.perform(get(CONNECTORS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("DM-A0001"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.tips").exists())
                .andExpect(jsonPath("$.dataType").value(nullValue()))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("订单库"))
                .andExpect(jsonPath("$.data[0].type").value("MySQL"))
                .andExpect(jsonPath("$.data[0].status").value("draft"));
    }

    // ---------- GET /api/connectors/stats ----------

    @Test
    @DisplayName("统计接口返回 200 与各数据源类型聚合计数")
    void statsReturns200WithAggregatedCounts() throws Exception {
        createConnector("{\"name\":\"主库\",\"type\":\"MySQL\",\"host\":\"10.0.0.1\",\"port\":3306,"
                + "\"dialect\":\"native\",\"username\":\"root\",\"password\":\"pwd\",\"autoClassify\":true}");
        createConnector("{\"name\":\"从库\",\"type\":\"MySQL\",\"host\":\"10.0.0.2\",\"port\":3306,"
                + "\"dialect\":\"native\",\"username\":\"root\",\"password\":\"pwd\",\"autoClassify\":true}");
        createConnector("{\"name\":\"Oracle生产\",\"type\":\"Oracle\",\"host\":\"10.0.0.3\",\"port\":1521,"
                + "\"dialect\":\"native\",\"username\":\"sys\",\"password\":\"pwd\",\"autoClassify\":true}");

        mockMvc.perform(get(CONNECTORS_PATH + "/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("DM-A0001"))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    // ---------- POST /api/connectors ----------

    @Test
    @DisplayName("新增连接器返回 201 与连接器数据（草稿状态）")
    void createReturns201WithConnectorData() throws Exception {
        mockMvc.perform(post(CONNECTORS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"订单库\",\"type\":\"MySQL\",\"host\":\"10.0.0.1\",\"port\":3306,"
                                + "\"dialect\":\"native\",\"username\":\"root\",\"password\":\"pwd-123\","
                                + "\"autoClassify\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("DM-A0001"))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("订单库"))
                .andExpect(jsonPath("$.data.type").value("MySQL"))
                .andExpect(jsonPath("$.data.host").value("10.0.0.1"))
                .andExpect(jsonPath("$.data.port").value(3306))
                .andExpect(jsonPath("$.data.dialect").value("native"))
                .andExpect(jsonPath("$.data.status").value("draft"));
    }

    @Test
    @DisplayName("新增重名连接器返回 409 与错误体结构（code/message/severity/fieldErrors）")
    void createDuplicateNameReturns409() throws Exception {
        createConnector("{\"name\":\"订单库\",\"type\":\"MySQL\",\"host\":\"10.0.0.1\",\"port\":3306,"
                + "\"dialect\":\"native\"}");

        mockMvc.perform(post(CONNECTORS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"订单库\",\"type\":\"MySQL\",\"host\":\"10.0.0.2\",\"port\":3307,"
                                + "\"dialect\":\"native\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("connector.name_conflict"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.severity").value("error"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    @Test
    @DisplayName("参数校验失败返回 422 且 fieldErrors 含字段级错误")
    void createValidationErrorReturns422WithFieldErrors() throws Exception {
        mockMvc.perform(post(CONNECTORS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"MySQL\",\"host\":\"10.0.0.1\",\"port\":3306,\"dialect\":\"native\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("param.invalid"))
                .andExpect(jsonPath("$.severity").value("error"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].message").exists());
    }

    @Test
    @DisplayName("非法枚举值（type 不在冻结枚举内）返回 422")
    void createInvalidEnumReturns422() throws Exception {
        mockMvc.perform(post(CONNECTORS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"订单库\",\"type\":\"NotExists\",\"host\":\"10.0.0.1\",\"port\":3306,"
                                + "\"dialect\":\"native\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.severity").value("error"))
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    // ---------- PUT /api/connectors/{id} ----------

    @Test
    @DisplayName("更新连接器返回 200 与更新后数据（配置变更后状态重置草稿）")
    void updateReturns200WithUpdatedData() throws Exception {
        ConnectorVO created = createConnector("{\"name\":\"订单库\",\"type\":\"MySQL\","
                + "\"host\":\"10.0.0.1\",\"port\":3306,\"dialect\":\"native\"}");

        mockMvc.perform(put(CONNECTORS_PATH + "/" + created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + created.getId() + "\",\"name\":\"订单库\",\"type\":\"MySQL\","
                                + "\"host\":\"10.0.0.9\",\"port\":3307,\"dialect\":\"native\","
                                + "\"username\":\"admin\",\"autoClassify\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.host").value("10.0.0.9"))
                .andExpect(jsonPath("$.data.port").value(3307))
                .andExpect(jsonPath("$.data.status").value("draft"));
    }

    @Test
    @DisplayName("更新不存在的连接器返回 404 错误体")
    void updateNotFoundReturns404() throws Exception {
        mockMvc.perform(put(CONNECTORS_PATH + "/not-exist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"not-exist\",\"name\":\"订单库\",\"type\":\"MySQL\","
                                + "\"host\":\"10.0.0.1\",\"port\":3306,\"dialect\":\"native\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("connector.not_found"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.severity").value("error"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    // ---------- DELETE /api/connectors/{id} ----------

    @Test
    @DisplayName("删除连接器返回 204 无响应体")
    void deleteReturns204() throws Exception {
        ConnectorVO created = createConnector("{\"name\":\"订单库\",\"type\":\"MySQL\","
                + "\"host\":\"10.0.0.1\",\"port\":3306,\"dialect\":\"native\"}");

        mockMvc.perform(delete(CONNECTORS_PATH + "/" + created.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(CONNECTORS_PATH))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("删除不存在的连接器返回 404")
    void deleteNotFoundReturns404() throws Exception {
        mockMvc.perform(delete(CONNECTORS_PATH + "/not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("连接器被采集任务引用时删除返回 409 与错误体结构")
    void deleteReferencedReturns409() throws Exception {
        ConnectorVO created = createConnector("{\"name\":\"订单库\",\"type\":\"MySQL\","
                + "\"host\":\"10.0.0.1\",\"port\":3306,\"dialect\":\"native\"}");
        CollectorTask task = CollectorTask.builder()
                .id("t-1")
                .name("每日采集")
                .connectorId(created.getId())
                .schedule(new CollectSchedule("0 0 2 * * ?"))
                .mode(CollectorMode.INCREMENTAL)
                .strategy(CollectorStrategy.IGNORE)
                .autoClassify(Boolean.TRUE)
                .status(CollectorTaskStatus.PENDING)
                .build();
        taskRepository.save(task);

        mockMvc.perform(delete(CONNECTORS_PATH + "/" + created.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("connector.in_use"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.severity").value("error"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
        // 连接器未被删除
        mockMvc.perform(get(CONNECTORS_PATH))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    // ---------- POST /api/connectors/{id}/test ----------

    @Test
    @DisplayName("测试连接成功返回 200，data.connected=true，状态已连接")
    void testConnectionSuccessReturns200() throws Exception {
        ConnectorVO created = createConnector("{\"name\":\"订单库\",\"type\":\"MySQL\","
                + "\"host\":\"10.0.0.1\",\"port\":3306,\"dialect\":\"native\"}");
        connectorTestSpi.enqueue(ConnectTestResult.success("连接成功"));

        mockMvc.perform(post(CONNECTORS_PATH + "/" + created.getId() + "/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("DM-A0001"))
                .andExpect(jsonPath("$.data.connected").value(true))
                .andExpect(jsonPath("$.data.message").value("连接成功"));
    }

    @Test
    @DisplayName("网络失败返回 422 且错误分类为 err.connector.network")
    void testNetworkFailureReturns422NetworkCode() throws Exception {
        ConnectorVO created = createConnector("{\"name\":\"订单库\",\"type\":\"MySQL\","
                + "\"host\":\"10.0.0.1\",\"port\":3306,\"dialect\":\"native\"}");
        connectorTestSpi.enqueue(ConnectTestResult.failure(ConnectErrorType.NETWORK, "无法连接到主机，请检查网络"));

        mockMvc.perform(post(CONNECTORS_PATH + "/" + created.getId() + "/test"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("err.connector.network"))
                .andExpect(jsonPath("$.message", containsString("无法连接")))
                .andExpect(jsonPath("$.severity").value("error"))
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    @DisplayName("凭据失败返回 422 且错误分类为 err.connector.credential（含 password 字段错误）")
    void testCredentialFailureReturns422CredentialCode() throws Exception {
        ConnectorVO created = createConnector("{\"name\":\"订单库\",\"type\":\"MySQL\","
                + "\"host\":\"10.0.0.1\",\"port\":3306,\"dialect\":\"native\"}");
        connectorTestSpi.enqueue(ConnectTestResult.failure(ConnectErrorType.CREDENTIAL, "用户名或密码不正确"));

        mockMvc.perform(post(CONNECTORS_PATH + "/" + created.getId() + "/test"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("err.connector.credential"))
                .andExpect(jsonPath("$.severity").value("error"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("err.credential.invalid"));
    }

    @Test
    @DisplayName("方言失败返回 422 且错误分类为 err.connector.dialect")
    void testDialectFailureReturns422DialectCode() throws Exception {
        ConnectorVO created = createConnector("{\"name\":\"订单库\",\"type\":\"MySQL\","
                + "\"host\":\"10.0.0.1\",\"port\":3306,\"dialect\":\"native\"}");
        connectorTestSpi.enqueue(ConnectTestResult.failure(ConnectErrorType.DIALECT, "方言不受支持"));

        mockMvc.perform(post(CONNECTORS_PATH + "/" + created.getId() + "/test"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("err.connector.dialect"))
                .andExpect(jsonPath("$.severity").value("error"))
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    @DisplayName("获取数据源服务系统名录成功返回名录列表")
    void testGetSystemCatalog() throws Exception {
        mockMvc.perform(get(CONNECTORS_PATH + "/systems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].code").exists())
                .andExpect(jsonPath("$.data[0].name").exists());
    }

    @Test
    @DisplayName("获取指定数据源下的 Database 列表成功返回")
    void testGetDatabases() throws Exception {
        mockMvc.perform(get(CONNECTORS_PATH + "/c-test-1/databases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0]").value("db_trade_core"));
    }

    private ConnectorVO createConnector(String json) throws Exception {
        String response = mockMvc.perform(post(CONNECTORS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return MAPPER.readValue(MAPPER.readTree(response).path("data").toString(), ConnectorVO.class);
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();
}
