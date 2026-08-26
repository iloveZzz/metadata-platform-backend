package com.yss.metadata.domain.lineage.gateway;

import com.yss.metadata.domain.lineage.model.ExportTask;

import java.util.Optional;

/**
 * 导出任务仓储端口（血缘域；Domain 定义，Infrastructure 实现）。
 *
 * <p>export_task 表：进行中任务查询（幂等复用判定）与保存（含状态流转）。</p>
 */
public interface ExportTaskGateway {

    /**
     * 查询同资产同格式的进行中任务（pending/running；幂等复用依据）。
     */
    Optional<ExportTask> findInProgress(String assetId, String format);

    /**
     * 保存导出任务（新增或更新状态）。
     */
    ExportTask save(ExportTask task);
}
