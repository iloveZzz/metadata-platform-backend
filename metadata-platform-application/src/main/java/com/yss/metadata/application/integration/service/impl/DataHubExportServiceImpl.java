package com.yss.metadata.application.integration.service.impl;

import com.yss.metadata.application.integration.service.DataHubExportService;
import com.yss.metadata.application.lineage.service.convertor.LineageAppConvertor;
import com.yss.metadata.client.vo.ExportTaskVO;
import com.yss.metadata.domain.audit.gateway.AuditLogGateway;
import com.yss.metadata.domain.audit.model.AuditLogEntry;
import com.yss.metadata.domain.connector.spi.CredentialCipher;
import com.yss.metadata.domain.integration.gateway.IntegrationConfigGateway;
import com.yss.metadata.domain.integration.model.DataHubEndpoint;
import com.yss.metadata.domain.integration.model.DataHubExportResult;
import com.yss.metadata.domain.integration.model.IntegrationConfig;
import com.yss.metadata.domain.integration.spi.DataHubExporter;
import com.yss.metadata.domain.lineage.gateway.ExportTaskGateway;
import com.yss.metadata.domain.lineage.model.ExportTask;
import com.yss.metadata.domain.lineage.model.ExportTaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * DataHub 导出应用服务实现（WU-05-04）。
 *
 * <p>202 异步任务幂等（复用 export_task：asset_id=NULL 全局导出 + format=datahub，
 * 同 asset_id+format 进行中任务复用）→ DataHubExporter SPI（防腐层；真实客户端
 * seam-deferred）→ 状态流转 pending→running→success/failed → 审计
 * （integration.datahub-export）。目标未配置抛非法参数（422）。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataHubExportServiceImpl implements DataHubExportService {

    /** DataHub 导出格式（export_task.format 扩展值） */
    public static final String FORMAT_DATAHUB = "datahub";

    /** DataHub 导出审计动作 */
    private static final String AUDIT_ACTION_EXPORT_DATAHUB = "integration.datahub-export";

    private final IntegrationConfigGateway integrationConfigGateway;
    private final ExportTaskGateway exportTaskRepository;
    private final DataHubExporter dataHubExporter;
    private final CredentialCipher credentialCipher;
    private final AuditLogGateway auditLogRepository;
    private final LineageAppConvertor lineageAppConvertor;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExportTaskVO trigger(String operator) {
        IntegrationConfig config = integrationConfigGateway.find()
                .orElseThrow(() -> new IllegalArgumentException("未配置 DataHub 导出目标"));
        String endpoint = config.getDatahubEndpoint();
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalArgumentException("未配置 DataHub 导出目标");
        }

        // 幂等：同 asset_id(NULL=全局导出) + format=datahub 进行中任务复用（202 返回既有任务）
        Optional<ExportTask> inProgress = exportTaskRepository.findInProgress(null, FORMAT_DATAHUB);
        if (inProgress.isPresent()) {
            log.info("DataHub 导出任务幂等复用，taskId={}, operator={}",
                    inProgress.get().getId(), operator);
            return lineageAppConvertor.toExportTaskVO(inProgress.get());
        }

        ExportTask task = ExportTask.builder()
                .id(UUID.randomUUID().toString())
                .assetId(null)
                .format(FORMAT_DATAHUB)
                .status(ExportTaskStatus.PENDING)
                .operator(operator)
                .createdAt(LocalDateTime.now())
                .build();
        exportTaskRepository.save(task);
        log.info("DataHub 导出任务已创建，taskId={}, operator={}", task.getId(), operator);

        ExportTask executed = execute(task, config);
        ExportTask finalTask = exportTaskRepository.save(executed);

        auditLogRepository.record(AuditLogEntry.builder()
                .id(UUID.randomUUID().toString())
                .operator(operator)
                .action(AUDIT_ACTION_EXPORT_DATAHUB)
                .object(finalTask.getId())
                .result(finalTask.getStatus().getValue())
                .time(LocalDateTime.now())
                .build());
        log.info("DataHub 导出审计已记录，taskId={}, status={}", finalTask.getId(), finalTask.getStatus().getValue());
        return lineageAppConvertor.toExportTaskVO(finalTask);
    }

    /**
     * 执行导出：running → DataHubExporter（防腐层）→ success/failed（任务状态承载，不抛异常）。
     */
    private ExportTask execute(ExportTask task, IntegrationConfig config) {
        task.setStatus(ExportTaskStatus.RUNNING);
        try {
            exportTaskRepository.save(task);
            DataHubEndpoint endpoint = DataHubEndpoint.builder()
                    .endpoint(config.getDatahubEndpoint())
                    .authToken(decrypt(config.getDatahubAuthRef()))
                    .build();
            DataHubExportResult result = dataHubExporter.export(endpoint, task.getOperator());
            task.setFileRef(result.getMessage());
            task.setStatus(result.isSuccess() ? ExportTaskStatus.SUCCESS : ExportTaskStatus.FAILED);
        } catch (Exception e) {
            log.error("DataHub 导出执行失败，taskId={}", task.getId(), e);
            task.setStatus(ExportTaskStatus.FAILED);
        }
        task.setFinishedAt(LocalDateTime.now());
        return task;
    }

    private String decrypt(String authRef) {
        return StringUtils.hasText(authRef) ? credentialCipher.decrypt(authRef) : null;
    }
}
