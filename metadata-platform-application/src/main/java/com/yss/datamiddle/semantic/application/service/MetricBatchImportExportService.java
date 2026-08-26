package com.yss.datamiddle.semantic.application.service;

import com.yss.datamiddle.semantic.application.port.CurrentUserPort;
import com.yss.datamiddle.semantic.metric.batch.MetricCsvParser;
import com.yss.datamiddle.semantic.metric.batch.MetricImportError;
import com.yss.datamiddle.semantic.metric.batch.MetricImportItem;
import com.yss.datamiddle.semantic.metric.batch.MetricImportResult;
import com.yss.datamiddle.semantic.metric.gateway.MetricGateway;
import com.yss.datamiddle.semantic.metric.model.MetricDefinition;
import com.yss.datamiddle.semantic.term.exception.PermissionDeniedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 指标口径批量导入与导出应用服务
 */
@Service
public class MetricBatchImportExportService {

    private final MetricGateway metricGateway;
    private final CurrentUserPort currentUserPort;
    private final MetricCsvParser csvParser = new MetricCsvParser();

    public MetricBatchImportExportService(
            MetricGateway metricGateway,
            CurrentUserPort currentUserPort
    ) {
        this.metricGateway = metricGateway;
        this.currentUserPort = currentUserPort;
    }

    /**
     * 批量导入指标口径
     */
    @Transactional(rollbackFor = Exception.class)
    public MetricImportResult importFromCsv(String csvContent, boolean overwriteExisting) {
        if (!currentUserPort.isWritePermitted()) {
            throw new PermissionDeniedException("当前用户为只读角色，无批量导入权限");
        }

        List<MetricImportItem> items = csvParser.parseCsv(csvContent);
        if (items.isEmpty()) {
            return MetricImportResult.builder()
                    .totalCount(0)
                    .successCount(0)
                    .failureCount(0)
                    .errors(Collections.emptyList())
                    .build();
        }

        int success = 0;
        int failure = 0;
        List<MetricImportError> errors = new ArrayList<>();
        String operator = currentUserPort.userName() != null ? currentUserPort.userName() : "system";

        for (MetricImportItem item : items) {
            String name = item.getName();
            if (name == null || name.trim().isEmpty()) {
                failure++;
                errors.add(MetricImportError.builder()
                        .rowNumber(item.getRowNumber())
                        .metricName(name)
                        .errorCode("NAME_REQUIRED")
                        .errorMessage("第 " + item.getRowNumber() + " 行指标名称不能为空")
                        .build());
                continue;
            }

            Optional<MetricDefinition> existingOpt = metricGateway.findByName(name.trim());
            if (existingOpt.isPresent()) {
                if (!overwriteExisting) {
                    failure++;
                    errors.add(MetricImportError.builder()
                            .rowNumber(item.getRowNumber())
                            .metricName(name)
                            .errorCode("METRIC_ALREADY_EXISTS")
                            .errorMessage("指标已存在: " + name)
                            .build());
                    continue;
                }

                // 覆盖模式：追加新版本
                MetricDefinition existing = existingOpt.get();
                if (item.getMetricGroup() != null && !item.getMetricGroup().isEmpty()) {
                    existing.setMetricGroup(item.getMetricGroup());
                }
                if (item.getDescription() != null) {
                    existing.setDescription(item.getDescription());
                }
                if (item.getOwner() != null && !item.getOwner().isEmpty()) {
                    existing.setOwner(item.getOwner());
                }

                if ((item.getExpression() != null && !item.getExpression().isEmpty())
                        || (item.getLogicDescription() != null && !item.getLogicDescription().isEmpty())) {
                    existing.addVersion(
                            item.getExpression() != null ? item.getExpression() : "",
                            item.getLogicDescription() != null ? item.getLogicDescription() : "",
                            Collections.emptyList(),
                            null,
                            operator
                    );
                }
                metricGateway.update(existing);
                success++;
            } else {
                // 新建指标
                MetricDefinition newMetric = MetricDefinition.create(
                        name.trim(),
                        item.getMetricGroup() != null ? item.getMetricGroup() : "DEFAULT",
                        item.getDescription() != null ? item.getDescription() : "",
                        item.getOwner() != null ? item.getOwner() : operator,
                        operator
                );

                if ((item.getExpression() != null && !item.getExpression().isEmpty())
                        || (item.getLogicDescription() != null && !item.getLogicDescription().isEmpty())) {
                    newMetric.addVersion(
                            item.getExpression() != null ? item.getExpression() : "",
                            item.getLogicDescription() != null ? item.getLogicDescription() : "",
                            Collections.emptyList(),
                            null,
                            operator
                    );
                }
                metricGateway.save(newMetric);
                success++;
            }
        }

        return MetricImportResult.builder()
                .totalCount(items.size())
                .successCount(success)
                .failureCount(failure)
                .errors(errors)
                .build();
    }

    /**
     * 批量导出全部指标口径为 CSV 字符串
     */
    @Transactional(readOnly = true)
    public String exportToCsv() {
        List<MetricDefinition> metrics = metricGateway.listAll();
        return csvParser.exportToCsv(metrics);
    }
}
