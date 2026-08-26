package com.yss.metadata.repository;

import com.yss.metadata.domain.lineage.gateway.ExportTaskGateway;
import com.yss.metadata.domain.lineage.model.ExportTask;
import com.yss.metadata.domain.lineage.model.ExportTaskStatus;
import com.yss.metadata.infrastructure.convertor.ExportTaskConvertor;
import com.yss.metadata.repository.gateway.impl.ExportTaskGatewayImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 导出任务仓储 H2 持久化测试（WU-03-04；export_task 表 + 幂等判定；
 * 切片 05 扩展 asset_id NULL 全局导出幂等判定）。
 */
class ExportTaskGatewayImplH2Test extends H2MapperTestSupport {

    private ExportTaskGateway exportTaskGateway;
    private ExportTaskRepository mapper;

    @BeforeEach
    void setUp() {
        mapper = sqlSession.getMapper(ExportTaskRepository.class);
        exportTaskGateway = new ExportTaskGatewayImpl(mapper, Mappers.getMapper(ExportTaskConvertor.class));
    }

    @Test
    @DisplayName("保存新任务（insert）→ 进行中任务可被幂等复用判定")
    void saveAndFindInProgress() {
        ExportTask task = task("task-1", "a-1", "csv", ExportTaskStatus.PENDING, "u-me");
        exportTaskGateway.save(task);

        Optional<ExportTask> found = exportTaskGateway.findInProgress("a-1", "csv");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo("task-1");
        assertThat(found.get().getStatus()).isEqualTo(ExportTaskStatus.PENDING);
        assertThat(mapper.selectById("task-1")).isNotNull();
    }

    @Test
    @DisplayName("状态流转 save 更新：running → success 幂等持久化同一行")
    void statusTransitionUpdates() {
        ExportTask task = task("task-1", "a-1", "json", ExportTaskStatus.PENDING, "u-me");
        exportTaskGateway.save(task);

        task.setStatus(ExportTaskStatus.RUNNING);
        exportTaskGateway.save(task);
        task.setStatus(ExportTaskStatus.SUCCESS);
        task.setFileRef("/tmp/task-1.json");
        task.setFinishedAt(LocalDateTime.now());
        exportTaskGateway.save(task);

        Optional<ExportTask> inProgress = exportTaskGateway.findInProgress("a-1", "json");
        assertThat(inProgress).isEmpty();
        com.yss.metadata.repository.entity.ExportTaskPO po = mapper.selectById("task-1");
        assertThat(po.getStatus()).isEqualTo("success");
        assertThat(po.getFileRef()).isEqualTo("/tmp/task-1.json");
    }

    @Test
    @DisplayName("幂等判定：不同资产或不同格式的进行中任务不误复用；success 任务不视为进行中")
    void findInProgressScopedByAssetAndFormat() {
        exportTaskGateway.save(task("t1", "a-1", "csv", ExportTaskStatus.PENDING, "u-me"));
        exportTaskGateway.save(task("t2", "a-2", "csv", ExportTaskStatus.RUNNING, "u-me"));
        exportTaskGateway.save(task("t3", "a-1", "json", ExportTaskStatus.RUNNING, "u-me"));
        exportTaskGateway.save(task("t4", "a-1", "csv", ExportTaskStatus.SUCCESS, "u-me"));

        assertThat(exportTaskGateway.findInProgress("a-1", "csv"))
                .isPresent().get().extracting(ExportTask::getId).isEqualTo("t1");
        assertThat(exportTaskGateway.findInProgress("a-2", "csv"))
                .isPresent().get().extracting(ExportTask::getId).isEqualTo("t2");
        assertThat(exportTaskGateway.findInProgress("a-9", "csv")).isEmpty();
    }

    @Test
    @DisplayName("切片 05：asset_id NULL 全局导出（DataHub）幂等判定——只命中 asset_id IS NULL 的进行中任务")
    void findInProgressNullAssetIdScoped() {
        exportTaskGateway.save(task("t1", "a-1", "datahub", ExportTaskStatus.PENDING, "u-me"));
        exportTaskGateway.save(task("t2", null, "datahub", ExportTaskStatus.RUNNING, "u-me"));
        exportTaskGateway.save(task("t3", null, "csv", ExportTaskStatus.RUNNING, "u-me"));
        exportTaskGateway.save(task("t4", null, "datahub", ExportTaskStatus.SUCCESS, "u-me"));

        // 全局导出（asset_id IS NULL）：只命中 t2（进行中），不误命中具体资产任务 t1 / 其他格式 t3
        assertThat(exportTaskGateway.findInProgress(null, "datahub"))
                .isPresent().get().extracting(ExportTask::getId).isEqualTo("t2");
        // 具体资产导出不误命中全局导出任务
        assertThat(exportTaskGateway.findInProgress("a-1", "datahub"))
                .isPresent().get().extracting(ExportTask::getId).isEqualTo("t1");
        // success 的全局导出任务不视为进行中
        assertThat(exportTaskGateway.findInProgress(null, "datahub"))
                .isPresent().get().extracting(ExportTask::getId).isEqualTo("t2");
    }

    private ExportTask task(String id, String assetId, String format,
                            ExportTaskStatus status, String operator) {
        return ExportTask.builder().id(id).assetId(assetId).format(format).status(status)
                .operator(operator).createdAt(LocalDateTime.now()).build();
    }
}
