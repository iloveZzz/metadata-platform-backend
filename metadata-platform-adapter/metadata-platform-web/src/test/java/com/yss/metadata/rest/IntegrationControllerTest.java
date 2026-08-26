package com.yss.metadata.rest;

import com.yss.metadata.application.integration.service.convertor.IntegrationAppConvertor;
import com.yss.metadata.application.integration.service.impl.IntegrationServiceImpl;
import com.yss.metadata.domain.connector.model.ConnectErrorType;
import com.yss.metadata.domain.connector.model.ConnectTestResult;
import com.yss.metadata.domain.integration.model.IntegrationConfig;
import com.yss.metadata.domain.integration.model.OpenLineageEventRecord;
import com.yss.metadata.domain.integration.model.OpenLineageParseStatus;
import com.yss.metadata.rest.advice.MetadataGlobalExceptionHandler;
import com.yss.metadata.rest.support.FakeGravitinoGateway;
import com.yss.metadata.rest.support.InMemoryAuditLogRepository;
import com.yss.metadata.rest.support.InMemoryIntegrationConfigGateway;
import com.yss.metadata.rest.support.InMemoryOpenLineageEventGateway;
import com.yss.metadata.rest.support.TestCredentialCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 集成配置 REST 契约测试（WU-05-05，冻结 OpenAPI integrations 段）。
 *
 * <p>覆盖：GET 200（空配置空结构非错误 / 组合 VO 映射）、PUT 200（保存 + 审计 +
 * 凭据加密引用 / test=true 连接成功 lastTest）、PUT 422（连接测试失败分类不保存）。</p>
 */
class IntegrationControllerTest {

    private InMemoryIntegrationConfigGateway configGateway;
    private InMemoryOpenLineageEventGateway eventGateway;
    private FakeGravitinoGateway gravitinoGateway;
    private InMemoryAuditLogRepository auditLogRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        configGateway = new InMemoryIntegrationConfigGateway();
        eventGateway = new InMemoryOpenLineageEventGateway();
        gravitinoGateway = new FakeGravitinoGateway();
        auditLogRepository = new InMemoryAuditLogRepository();
        IntegrationServiceImpl service = new IntegrationServiceImpl(configGateway, eventGateway,
                gravitinoGateway, new TestCredentialCipher(), auditLogRepository, org.mapstruct.factory.Mappers.getMapper(IntegrationAppConvertor.class));
        mockMvc = MockMvcBuilders.standaloneSetup(new IntegrationController(service))
                .setControllerAdvice(new MetadataGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/integrations 空配置：200 空结构（Gravitino/DataHub 空对象 + OpenLineage 端点与 0 统计）")
    void getEmptyConfigReturns200EmptyStructure() throws Exception {
        mockMvc.perform(get("/api/integrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gravitino.endpoint").doesNotExist())
                .andExpect(jsonPath("$.data.datahub.endpoint").doesNotExist())
                .andExpect(jsonPath("$.data.openLineage.receiveEndpoint").value("/api/v1/lineage"))
                .andExpect(jsonPath("$.data.openLineage.recent24h").value(0));
    }

    @Test
    @DisplayName("GET /api/integrations 已配置：200 组合 VO（含事件统计）")
    void getConfiguredReturns200() throws Exception {
        configGateway.seed(IntegrationConfig.builder()
                .id(IntegrationConfig.SINGLETON_ID)
                .gravitinoEndpoint("http://gravitino:8090")
                .gravitinoEnabled(true)
                .datahubEndpoint("http://datahub:8080")
                .build());
        eventGateway.save(record("e1", OpenLineageParseStatus.PARSED));
        eventGateway.save(record("e2", OpenLineageParseStatus.PARSE_FAILED));

        mockMvc.perform(get("/api/integrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gravitino.endpoint").value("http://gravitino:8090"))
                .andExpect(jsonPath("$.data.gravitino.enabled").value(true))
                .andExpect(jsonPath("$.data.datahub.endpoint").value("http://datahub:8080"))
                .andExpect(jsonPath("$.data.openLineage.recent24h").value(2))
                .andExpect(jsonPath("$.data.openLineage.parseSuccessRate").value("50.0%"));
    }

    @Test
    @DisplayName("PUT /api/integrations 仅保存：200 + 审计 + 凭据不落明文")
    void putSaveReturns200() throws Exception {
        mockMvc.perform(put("/api/integrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "u-me")
                        .content("{\"gravitinoEndpoint\":\"http://gravitino:8090\","
                                + "\"gravitinoAuthToken\":\"tok-secret\","
                                + "\"gravitinoEnabled\":true,"
                                + "\"datahubEndpoint\":\"http://datahub:8080\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gravitino.endpoint").value("http://gravitino:8090"))
                .andExpect(jsonPath("$.data.gravitino.enabled").value(true))
                .andExpect(jsonPath("$.data.datahub.endpoint").value("http://datahub:8080"));

        IntegrationConfig saved = configGateway.find().get();
        assertThat(saved.getGravitinoAuthRef()).doesNotContain("tok-secret");
        assertThat(auditLogRepository.entries()).hasSize(1);
        assertThat(auditLogRepository.entries().get(0).getAction()).isEqualTo("integration.config");
        assertThat(auditLogRepository.entries().get(0).getOperator()).isEqualTo("u-me");
    }

    @Test
    @DisplayName("PUT /api/integrations test=true 连接成功：200 + lastTest 连接测试通过")
    void putWithTestSuccessReturns200() throws Exception {
        gravitinoGateway.setResult(ConnectTestResult.success("connected"));

        mockMvc.perform(put("/api/integrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gravitinoEndpoint\":\"http://gravitino:8090\",\"test\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gravitino.lastTest").value(org.hamcrest.Matchers.containsString("连接测试通过")));
    }

    @Test
    @DisplayName("PUT /api/integrations test=true 网络失败：422 err.connector.network，不保存")
    void putWithTestNetworkFailureReturns422() throws Exception {
        gravitinoGateway.setResult(ConnectTestResult.failure(ConnectErrorType.NETWORK, "无法连接 Gravitino"));

        mockMvc.perform(put("/api/integrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gravitinoEndpoint\":\"http://gravitino:8090\",\"test\":true}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("err.connector.network"))
                .andExpect(jsonPath("$.message").value("无法连接 Gravitino"));

        assertThat(configGateway.find()).isEmpty();
    }

    @Test
    @DisplayName("PUT /api/integrations test=true 凭据失败：422 err.connector.credential + password fieldErrors")
    void putWithTestCredentialFailureReturns422() throws Exception {
        gravitinoGateway.setResult(ConnectTestResult.failure(ConnectErrorType.CREDENTIAL, "认证失败"));

        mockMvc.perform(put("/api/integrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gravitinoEndpoint\":\"http://gravitino:8090\",\"test\":true}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("err.connector.credential"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
    }

    @Test
    @DisplayName("切片 06：PUT /api/integrations 非管理员：403 rbac.forbidden（写路径管理端门禁；GET 浏览保持开放）")
    void putNonAdminReturns403() throws Exception {
        mockMvc.perform(put("/api/integrations")
                        .header("X-User-Role", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gravitinoEndpoint\":\"http://gravitino:8090\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("rbac.forbidden"));

        // 未保存
        assertThat(configGateway.find()).isEmpty();
    }

    private OpenLineageEventRecord record(String id, OpenLineageParseStatus status) {
        return OpenLineageEventRecord.builder()
                .id(id)
                .eventType("COMPLETE")
                .runId("run-" + id)
                .jobNamespace("ns")
                .jobName("job")
                .parseStatus(status)
                .receivedAt(LocalDateTime.now())
                .build();
    }
}
