package com.yss.metadata.application.integration.service;

import com.yss.metadata.application.connector.support.TestCredentialCipher;
import com.yss.metadata.application.integration.service.impl.DataHubExportServiceImpl;
import com.yss.metadata.application.integration.support.FakeDataHubExporter;
import com.yss.metadata.application.integration.support.InMemoryIntegrationConfigGateway;
import com.yss.metadata.application.lineage.service.convertor.LineageAppConvertor;
import com.yss.metadata.application.lineage.support.InMemoryAuditLogRepository;
import com.yss.metadata.application.lineage.support.InMemoryExportTaskRepository;
import com.yss.metadata.client.vo.ExportTaskVO;
import com.yss.metadata.domain.audit.model.AuditLogEntry;
import com.yss.metadata.domain.integration.model.DataHubExportResult;
import com.yss.metadata.domain.integration.model.IntegrationConfig;
import com.yss.metadata.domain.lineage.model.ExportTask;
import com.yss.metadata.domain.lineage.model.ExportTaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DataHub 导出应用服务测试（WU-05-04）。
 *
 * <p>覆盖：202 幂等复用（asset_id=NULL 全局导出 + format=datahub）、状态流转
 * pending→running→success/failed、审计 integration.datahub-export、目标未配置 422、
 * 凭据解密传入防腐层。</p>
 */
class DataHubExportServiceTest {

    private InMemoryIntegrationConfigGateway configGateway;
    private InMemoryExportTaskRepository exportTaskRepository;
    private FakeDataHubExporter dataHubExporter;
    private InMemoryAuditLogRepository auditLogRepository;
    private DataHubExportService service;

    @BeforeEach
    void setUp() {
        configGateway = new InMemoryIntegrationConfigGateway();
        exportTaskRepository = new InMemoryExportTaskRepository();
        dataHubExporter = new FakeDataHubExporter();
        auditLogRepository = new InMemoryAuditLogRepository();
        service = new DataHubExportServiceImpl(configGateway, exportTaskRepository, dataHubExporter,
                new TestCredentialCipher(), auditLogRepository, org.mapstruct.factory.Mappers.getMapper(LineageAppConvertor.class));
    }

    @Test
    @DisplayName("无集成配置抛非法参数（422 语义）")
    void noConfigThrows422() {
        assertThatThrownBy(() -> service.trigger("u-me"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DataHub");
    }

    @Test
    @DisplayName("未配置 DataHub 目标抛非法参数（422 语义）")
    void noDatahubEndpointThrows422() {
        configGateway.seed(IntegrationConfig.builder().id(IntegrationConfig.SINGLETON_ID).build());
        assertThatThrownBy(() -> service.trigger("u-me"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DataHub");
    }

    @Test
    @DisplayName("导出成功：任务 SUCCESS + fileRef 写结果 + 审计 integration.datahub-export(success)")
    void triggerSuccess() {
        configGateway.seed(configWithEndpoint());
        dataHubExporter.setResult(DataHubExportResult.success("已导出 12 个数据集"));

        ExportTaskVO vo = service.trigger("u-me");

        assertThat(vo.getStatus()).isEqualTo("success");
        assertThat(vo.getFileRef()).isEqualTo("已导出 12 个数据集");
        assertThat(vo.getFormat()).isEqualTo("datahub");
        assertThat(vo.getAssetId()).isNull();
        assertThat(vo.getFinishedAt()).isNotNull();
        // 幂等判定不误命中：成功任务不再视为进行中
        assertThat(exportTaskRepository.findInProgress(null, "datahub")).isEmpty();
        // 审计
        assertThat(auditLogRepository.entries()).hasSize(1);
        AuditLogEntry entry = auditLogRepository.entries().get(0);
        assertThat(entry.getAction()).isEqualTo("integration.datahub-export");
        assertThat(entry.getOperator()).isEqualTo("u-me");
        assertThat(entry.getResult()).isEqualTo("success");
        assertThat(entry.getObject()).isEqualTo(vo.getId());
        // 凭据解密传入防腐层
        assertThat(dataHubExporter.getCalls()).hasSize(1);
        assertThat(dataHubExporter.getCalls().get(0)).startsWith("http://datahub:8080@u-me");
    }

    @Test
    @DisplayName("导出失败：任务 FAILED（状态承载不抛异常）+ 审计 result=failed")
    void triggerFailureMarksTaskFailed() {
        configGateway.seed(configWithEndpoint());
        dataHubExporter.setResult(DataHubExportResult.failure("DataHub 认证失败"));

        ExportTaskVO vo = service.trigger("u-me");

        assertThat(vo.getStatus()).isEqualTo("failed");
        assertThat(vo.getFileRef()).isEqualTo("DataHub 认证失败");
        assertThat(auditLogRepository.entries()).hasSize(1);
        assertThat(auditLogRepository.entries().get(0).getResult()).isEqualTo("failed");
    }

    @Test
    @DisplayName("导出器抛异常：任务 FAILED 且不抛给调用方（任务状态承载）")
    void exporterExceptionMarksTaskFailed() {
        configGateway.seed(configWithEndpoint());
        // 模拟导出器抛运行时异常：改用真实抛错替身
        service = new DataHubExportServiceImpl(configGateway, exportTaskRepository, new ThrowingExporter(),
                new TestCredentialCipher(), auditLogRepository,
                org.mapstruct.factory.Mappers.getMapper(LineageAppConvertor.class));

        ExportTaskVO vo = service.trigger("u-me");

        assertThat(vo.getStatus()).isEqualTo("failed");
        assertThat(auditLogRepository.entries()).hasSize(1);
        assertThat(auditLogRepository.entries().get(0).getResult()).isEqualTo("failed");
    }

    @Test
    @DisplayName("202 幂等复用：存在进行中任务（asset_id NULL + format=datahub）返回既有任务，不新建不重复审计")
    void inProgressTaskReused() {
        configGateway.seed(configWithEndpoint());
        exportTaskRepository.seed(ExportTask.builder()
                .id("task-in-progress")
                .assetId(null)
                .format("datahub")
                .status(ExportTaskStatus.RUNNING)
                .operator("other")
                .createdAt(LocalDateTime.now())
                .build());

        ExportTaskVO vo = service.trigger("u-me");

        assertThat(vo.getId()).isEqualTo("task-in-progress");
        assertThat(vo.getStatus()).isEqualTo("running");
        assertThat(dataHubExporter.getCalls()).isEmpty();
        assertThat(auditLogRepository.entries()).isEmpty();
    }

    @Test
    @DisplayName("幂等不误命中：其他格式/非进行中任务不复用，创建新任务")
    void idempotencyScopedCorrectly() {
        configGateway.seed(configWithEndpoint());
        exportTaskRepository.seed(ExportTask.builder()
                .id("old-task")
                .assetId(null)
                .format("csv")
                .status(ExportTaskStatus.SUCCESS)
                .operator("other")
                .createdAt(LocalDateTime.now())
                .finishedAt(LocalDateTime.now())
                .build());
        dataHubExporter.setResult(DataHubExportResult.success("ok"));

        ExportTaskVO vo = service.trigger("u-me");

        assertThat(vo.getId()).isNotEqualTo("old-task");
        assertThat(vo.getStatus()).isEqualTo("success");
        assertThat(exportTaskRepository.all()).hasSize(2);
    }

    private IntegrationConfig configWithEndpoint() {
        return IntegrationConfig.builder()
                .id(IntegrationConfig.SINGLETON_ID)
                .datahubEndpoint("http://datahub:8080")
                .datahubAuthRef(new TestCredentialCipher().encrypt("dh-token"))
                .build();
    }

    /** 导出器抛异常替身（验证任务状态承载异常语义）。 */
    private static class ThrowingExporter implements com.yss.metadata.domain.integration.spi.DataHubExporter {
        @Override
        public com.yss.metadata.domain.integration.model.DataHubExportResult export(
                com.yss.metadata.domain.integration.model.DataHubEndpoint endpoint, String operator) {
            throw new IllegalStateException("DataHub 连接中断");
        }
    }
}
