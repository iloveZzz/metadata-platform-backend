package com.yss.metadata.rest;

import com.yss.metadata.application.integration.service.impl.DataHubExportServiceImpl;
import com.yss.metadata.application.lineage.service.convertor.LineageAppConvertor;
import com.yss.metadata.domain.integration.model.DataHubExportResult;
import com.yss.metadata.domain.integration.model.IntegrationConfig;
import com.yss.metadata.domain.lineage.model.ExportTask;
import com.yss.metadata.domain.lineage.model.ExportTaskStatus;
import com.yss.metadata.rest.advice.MetadataGlobalExceptionHandler;
import com.yss.metadata.rest.support.FakeDataHubExporter;
import com.yss.metadata.rest.support.InMemoryAuditLogRepository;
import com.yss.metadata.rest.support.InMemoryExportTaskRepository;
import com.yss.metadata.rest.support.InMemoryIntegrationConfigGateway;
import com.yss.metadata.rest.support.TestCredentialCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DataHub 导出 REST 契约测试（WU-05-05，冻结 OpenAPI /api/exports/datahub 段）。
 *
 * <p>覆盖：触发 202（ExportTask success/failed）、202 幂等复用（进行中任务）、
 * 目标未配置 422、审计记录。</p>
 */
class DataHubExportControllerTest {

    private InMemoryIntegrationConfigGateway configGateway;
    private InMemoryExportTaskRepository exportTaskRepository;
    private FakeDataHubExporter dataHubExporter;
    private InMemoryAuditLogRepository auditLogRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        configGateway = new InMemoryIntegrationConfigGateway();
        exportTaskRepository = new InMemoryExportTaskRepository();
        dataHubExporter = new FakeDataHubExporter();
        auditLogRepository = new InMemoryAuditLogRepository();
        DataHubExportServiceImpl service = new DataHubExportServiceImpl(configGateway, exportTaskRepository,
                dataHubExporter, new TestCredentialCipher(), auditLogRepository, org.mapstruct.factory.Mappers.getMapper(LineageAppConvertor.class));
        mockMvc = MockMvcBuilders.standaloneSetup(new DataHubExportController(service))
                .setControllerAdvice(new MetadataGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/exports/datahub 导出成功：202 + ExportTask（success，assetId 空）")
    void triggerSuccessReturns202() throws Exception {
        seedConfig();
        dataHubExporter.setResult(DataHubExportResult.success("已导出 12 个数据集"));

        mockMvc.perform(post("/api/exports/datahub").header("X-User-Id", "u-me"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.format").value("datahub"))
                .andExpect(jsonPath("$.data.status").value("success"))
                .andExpect(jsonPath("$.data.assetId").doesNotExist())
                .andExpect(jsonPath("$.data.fileRef").value("已导出 12 个数据集"));

        assertThat(auditLogRepository.entries()).hasSize(1);
        assertThat(auditLogRepository.entries().get(0).getAction()).isEqualTo("integration.datahub-export");
        assertThat(auditLogRepository.entries().get(0).getOperator()).isEqualTo("u-me");
    }

    @Test
    @DisplayName("POST /api/exports/datahub 导出失败：202 + ExportTask failed（任务状态承载）")
    void triggerFailureReturns202WithFailedTask() throws Exception {
        seedConfig();
        dataHubExporter.setResult(DataHubExportResult.failure("DataHub 认证失败"));

        mockMvc.perform(post("/api/exports/datahub"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("failed"))
                .andExpect(jsonPath("$.data.fileRef").value("DataHub 认证失败"));
    }

    @Test
    @DisplayName("POST /api/exports/datahub 202 幂等复用：进行中任务返回既有任务")
    void triggerReusesInProgressTask() throws Exception {
        seedConfig();
        exportTaskRepository.seed(ExportTask.builder()
                .id("task-in-progress")
                .assetId(null)
                .format("datahub")
                .status(ExportTaskStatus.RUNNING)
                .operator("other")
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(post("/api/exports/datahub"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.id").value("task-in-progress"))
                .andExpect(jsonPath("$.data.status").value("running"));
        assertThat(auditLogRepository.entries()).isEmpty();
    }

    @Test
    @DisplayName("POST /api/exports/datahub 目标未配置：422（asset.param.invalid）")
    void triggerWithoutConfigReturns422() throws Exception {
        mockMvc.perform(post("/api/exports/datahub"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("asset.param.invalid"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("DataHub")));
        assertThat(exportTaskRepository.all()).isEmpty();
    }

    private void seedConfig() {
        configGateway.seed(IntegrationConfig.builder()
                .id(IntegrationConfig.SINGLETON_ID)
                .datahubEndpoint("http://datahub:8080")
                .datahubAuthRef(new TestCredentialCipher().encrypt("dh-token"))
                .build());
    }
}
