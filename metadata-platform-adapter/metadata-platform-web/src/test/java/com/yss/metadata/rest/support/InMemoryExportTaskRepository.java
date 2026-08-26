package com.yss.metadata.rest.support;

import com.yss.metadata.domain.lineage.gateway.ExportTaskGateway;
import com.yss.metadata.domain.lineage.model.ExportTask;
import com.yss.metadata.domain.lineage.model.ExportTaskStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 导出任务仓储内存实现（Web 契约测试 seam）。
 */
public class InMemoryExportTaskRepository implements ExportTaskGateway {

    private final List<ExportTask> store = new ArrayList<>();

    public void seed(ExportTask task) {
        store.add(task);
    }

    public List<ExportTask> all() {
        return Collections.unmodifiableList(store);
    }

    @Override
    public Optional<ExportTask> findInProgress(String assetId, String format) {
        return store.stream()
                .filter(task -> java.util.Objects.equals(task.getAssetId(), assetId)
                        && task.getFormat().equals(format))
                .filter(task -> task.getStatus() == ExportTaskStatus.PENDING
                        || task.getStatus() == ExportTaskStatus.RUNNING)
                .findFirst();
    }

    @Override
    public ExportTask save(ExportTask task) {
        store.removeIf(existing -> existing.getId().equals(task.getId()));
        store.add(task);
        return task;
    }
}
