package com.yss.metadata.application.lineage.service.impl;

import com.yss.metadata.application.lineage.service.ImpactAnalysisService;
import com.yss.metadata.application.lineage.service.convertor.LineageAppConvertor;
import com.yss.metadata.application.lineage.service.support.ExportJsonWriter;
import com.yss.metadata.client.vo.ExportTaskVO;
import com.yss.metadata.client.vo.ImpactVO;
import com.yss.metadata.domain.asset.exception.AssetNotFoundException;
import com.yss.metadata.domain.asset.gateway.AssetRepository;
import com.yss.metadata.domain.audit.gateway.AuditLogGateway;
import com.yss.metadata.domain.audit.model.AuditLogEntry;
import com.yss.metadata.domain.lineage.gateway.ExportFileStorage;
import com.yss.metadata.domain.lineage.gateway.ExportTaskGateway;
import com.yss.metadata.domain.lineage.gateway.ImpactAnalysisRepository;
import com.yss.metadata.domain.lineage.model.ExportTask;
import com.yss.metadata.domain.lineage.model.ExportTaskStatus;
import com.yss.metadata.domain.lineage.model.ImpactNode;
import com.yss.metadata.domain.lineage.model.ImpactSort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * 影响分析应用服务实现（WU-03-03 / WU-03-04）。
 *
 * <p>影响分析：下游全量召回（基础设施递归 CTE，环保护 + 深度上限 10）→
 * 按深度分组（sortBy depth/domain/risk，默认 depth）；0 影响空结构（非错误）。</p>
 *
 * <p>导出任务：202 异步幂等（同资产同格式进行中任务复用）→ export_task 表
 * 状态流转 pending→running→success/failed → CSV/JSON 生成（本地可配置目录
 * seam，{@link ExportFileStorage}）→ audit_log 审计（impact.export）。
 * 生成异常标记 failed 并以 202 返回任务信息（客户端以任务状态为准）。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImpactAnalysisServiceImpl implements ImpactAnalysisService {

    /** 影响分析深度上限（环保护兜底；对齐合同 build_architecture_checklist） */
    public static final int MAX_IMPACT_DEPTH = 10;

    /** 导出审计动作 */
    private static final String AUDIT_ACTION_EXPORT = "impact.export";

    private final ImpactAnalysisRepository impactAnalysisRepository;
    private final ExportTaskGateway exportTaskRepository;
    private final ExportFileStorage exportFileStorage;
    private final AuditLogGateway auditLogRepository;
    private final AssetRepository assetRepository;
    private final LineageAppConvertor lineageAppConvertor;

    @Override
    @Transactional(readOnly = true)
    public ImpactVO getImpact(String assetId, String sortBy) {
        requireAsset(assetId);
        ImpactSort sort = ImpactSort.fromValue(sortBy);
        List<ImpactNode> nodes = impactAnalysisRepository.findDownstream(assetId, MAX_IMPACT_DEPTH);
        return lineageAppConvertor.toImpactVO(nodes, sort);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExportTaskVO exportImpact(String assetId, String format, String operator) {
        requireAsset(assetId);
        String normalizedFormat = normalizeFormat(format);

        // 幂等：同资产同格式进行中任务复用（202 返回既有任务）
        Optional<ExportTask> inProgress = exportTaskRepository.findInProgress(assetId, normalizedFormat);
        if (inProgress.isPresent()) {
            log.info("导出任务幂等复用，taskId={}, assetId={}, format={}, operator={}",
                    inProgress.get().getId(), assetId, normalizedFormat, operator);
            return lineageAppConvertor.toExportTaskVO(inProgress.get());
        }

        ExportTask task = ExportTask.builder()
                .id(UUID.randomUUID().toString())
                .assetId(assetId)
                .format(normalizedFormat)
                .status(ExportTaskStatus.PENDING)
                .operator(operator)
                .createdAt(LocalDateTime.now())
                .build();
        exportTaskRepository.save(task);
        log.info("导出任务已创建，taskId={}, assetId={}, format={}, operator={}",
                task.getId(), assetId, normalizedFormat, operator);

        ExportTask executed = execute(task);
        ExportTask finalTask = exportTaskRepository.save(executed);

        auditLogRepository.record(AuditLogEntry.builder()
                .id(UUID.randomUUID().toString())
                .operator(operator)
                .action(AUDIT_ACTION_EXPORT)
                .object(finalTask.getId())
                .result(finalTask.getStatus().getValue())
                .time(LocalDateTime.now())
                .build());
        log.info("导出审计已记录，taskId={}, status={}", finalTask.getId(), finalTask.getStatus().getValue());
        return lineageAppConvertor.toExportTaskVO(finalTask);
    }

    /**
     * 执行导出：running → 生成文件（CSV/JSON）→ success；失败 → failed（不抛异常，任务状态承载）。
     */
    private ExportTask execute(ExportTask task) {
        task.setStatus(ExportTaskStatus.RUNNING);
        try {
            exportTaskRepository.save(task);
            ImpactVO impact = getImpact(task.getAssetId(), "depth");
            String content = render(task.getId(), task.getFormat(), impact);
            String fileRef = exportFileStorage.store(task.getId(), task.getFormat(), content);
            task.setStatus(ExportTaskStatus.SUCCESS);
            task.setFileRef(fileRef);
        } catch (Exception e) {
            log.error("导出生成失败，taskId={}, assetId={}, format={}",
                    task.getId(), task.getAssetId(), task.getFormat(), e);
            task.setStatus(ExportTaskStatus.FAILED);
        }
        task.setFinishedAt(LocalDateTime.now());
        return task;
    }

    /**
     * 导出内容渲染：csv（首行表头 + 转义字段）或 json（ImpactVO 序列化，
     * 轻量渲染器 {@link ExportJsonWriter}，无外部 JSON 依赖）。
     */
    private String render(String taskId, String format, ImpactVO impact) {
        if ("json".equalsIgnoreCase(format)) {
            return ExportJsonWriter.toJson(impact);
        }
        return renderCsv(impact);
    }

    private String renderCsv(ImpactVO impact) {
        StringBuilder csv = new StringBuilder();
        csv.append("asset_id,name,type,domain,classification,risk,depth\n");
        impact.getGroups().forEach(group ->
                group.getItems().forEach(item ->
                        csv.append(escapeCsv(item.getAssetId())).append(',')
                                .append(escapeCsv(item.getName())).append(',')
                                .append(escapeCsv(item.getType())).append(',')
                                .append(escapeCsv(item.getDomain())).append(',')
                                .append(escapeCsv(item.getClassification())).append(',')
                                .append(escapeCsv(item.getRisk())).append(',')
                                .append(item.getDepth()).append('\n')));
        return csv.toString();
    }

    /**
     * CSV 字段转义：含逗号/引号/换行时加引号包裹，内部引号翻倍。
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    /**
     * format 参数归一化（csv/json；未知值抛非法参数 → 422）。
     */
    private String normalizeFormat(String format) {
        if (format == null || format.trim().isEmpty()) {
            return "csv";
        }
        String normalized = format.trim().toLowerCase(Locale.ROOT);
        if (!"csv".equals(normalized) && !"json".equals(normalized)) {
            throw new IllegalArgumentException("未知导出格式: " + format);
        }
        return normalized;
    }

    private void requireAsset(String assetId) {
        assetRepository.findById(assetId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));
    }
}
