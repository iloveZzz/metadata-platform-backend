package com.yss.metadata.application.collector.service;

import com.yss.metadata.application.collector.service.convertor.CollectorAppConvertor;
import com.yss.metadata.application.collector.service.impl.CollectorTaskAppServiceImpl;
import com.yss.metadata.application.collector.support.InMemoryCollectorTaskRepository;
import com.yss.metadata.client.dto.cmd.CollectorAddCmd;
import com.yss.metadata.client.dto.cmd.CollectorUpdateCmd;
import com.yss.metadata.client.vo.CollectorVO;
import com.yss.metadata.domain.collector.model.CollectorMode;
import com.yss.metadata.domain.collector.model.CollectorStrategy;
import com.yss.metadata.domain.collector.model.CollectorTask;
import com.yss.metadata.domain.collector.model.CollectorTaskStatus;
import com.yss.metadata.domain.collector.exception.CollectorTaskConflictException;
import com.yss.metadata.domain.collector.exception.CollectorTaskNotFoundException;
import com.yss.metadata.domain.collector.exception.CollectorTaskStateConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 采集任务应用服务用例测试（WU-01-02/03）。
 *
 * <p>覆盖：create 创建待执行、同数据源+调度唯一（409）、update 编辑调度、
 * start 幂等拒绝、cancel 仅运行中、markSucceeded/markFailed 状态流转与失败原因、
 * 不存在 404 语义。</p>
 */
class CollectorTaskAppServiceTest {

    private InMemoryCollectorTaskRepository repository;
    private CollectorTaskAppService appService;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCollectorTaskRepository();
        appService = new CollectorTaskAppServiceImpl(repository, org.mapstruct.factory.Mappers.getMapper(CollectorAppConvertor.class));
    }

    @Test
    @DisplayName("创建采集任务：初始待执行并持久化")
    void createThenPendingAndPersisted() {
        CollectorVO task = appService.create(buildAddCmd("每日元数据采集", "c-1", "0 0 2 * * ?"));

        assertThat(task.getId()).isNotBlank();
        assertThat(task.getStatus()).isEqualTo("pending");
        assertThat(task.getConnectorId()).isEqualTo("c-1");
        assertThat(task.getMode()).isEqualTo("incremental");
        assertThat(task.getStrategy()).isEqualTo("ignore");
        assertThat(task.getAutoClassify()).isTrue();
        assertThat(repository.findById(task.getId())).isPresent();
    }

    @Test
    @DisplayName("创建同数据源+同调度任务抛出冲突（409 幂等语义）")
    void createDuplicateConnectorAndScheduleRejected() {
        appService.create(buildAddCmd("任务一", "c-1", "0 0 2 * * ?"));

        assertThatThrownBy(() -> appService.create(buildAddCmd("任务二", "c-1", "0 0 2 * * ?")))
                .isInstanceOf(CollectorTaskConflictException.class)
                .hasMessageContaining("c-1");
    }

    @Test
    @DisplayName("同数据源不同调度允许创建")
    void createSameConnectorDifferentScheduleAllowed() {
        appService.create(buildAddCmd("任务一", "c-1", "0 0 2 * * ?"));

        appService.create(buildAddCmd("任务二", "c-1", "0 0 3 * * ?"));

        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("创建携带非法参数被领域不变量拒绝")
    void createWithInvalidParamsRejected() {
        CollectorAddCmd blankName = buildAddCmd(" ", "c-1", "0 0 2 * * ?");
        assertThatThrownBy(() -> appService.create(blankName))
                .isInstanceOf(IllegalArgumentException.class);

        CollectorAddCmd noSchedule = buildAddCmd("任务", "c-1", "  ");
        assertThatThrownBy(() -> appService.create(noSchedule))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("采集任务列表返回全部任务")
    void listReturnsAll() {
        appService.create(buildAddCmd("任务一", "c-1", "0 0 2 * * ?"));
        appService.create(buildAddCmd("任务二", "c-2", "0 0 3 * * ?"));

        assertThat(appService.list()).hasSize(2);
    }

    @Test
    @DisplayName("采集任务按条件过滤查询")
    void listWithQueryFilter() {
        CollectorAddCmd cmd1 = buildAddCmd("营销域增量采集", "c-1", "0 0 2 * * ?");
        cmd1.setOwner("1397905662202719");
        cmd1.setEnabled(Boolean.TRUE);
        cmd1.setDatasourceType("MySQL");
        appService.create(cmd1);

        CollectorAddCmd cmd2 = buildAddCmd("风控全量采集", "c-2", "0 0 3 * * ?");
        cmd2.setOwner("data_eng");
        cmd2.setEnabled(Boolean.FALSE);
        cmd2.setDatasourceType("Oracle");
        appService.create(cmd2);

        com.yss.metadata.client.dto.query.CollectorQuery query = com.yss.metadata.client.dto.query.CollectorQuery.builder()
                .keyword("营销")
                .owner("1397905662202719")
                .enabled(Boolean.TRUE)
                .build();
        assertThat(appService.list(query)).hasSize(1);
    }

    @Test
    @DisplayName("切换任务生效状态成功")
    void toggleStatusSuccess() {
        CollectorVO task = appService.create(buildAddCmd("每日元数据采集", "c-1", "0 0 2 * * ?"));
        assertThat(task.getEnabled()).isTrue();

        CollectorVO disabled = appService.toggleStatus(task.getId(), false);
        assertThat(disabled.getEnabled()).isFalse();

        CollectorVO enabled = appService.toggleStatus(task.getId(), true);
        assertThat(enabled.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("编辑采集任务调度成功：配置变更后重置待执行")
    void updateSuccess() {
        CollectorVO task = appService.create(buildAddCmd("每日元数据采集", "c-1", "0 0 2 * * ?"));
        appService.start(task.getId());
        appService.markFailed(task.getId(), "连接超时");

        CollectorUpdateCmd updateCmd = new CollectorUpdateCmd();
        updateCmd.setId(task.getId());
        updateCmd.setName("每日元数据采集-新");
        updateCmd.setConnectorId("c-1");
        updateCmd.setSchedule("0 0 4 * * ?");
        updateCmd.setMode(CollectorMode.FULL);
        updateCmd.setStrategy(CollectorStrategy.OVERWRITE);
        updateCmd.setAutoClassify(Boolean.FALSE);

        CollectorVO updated = appService.update(task.getId(), updateCmd);

        assertThat(updated.getName()).isEqualTo("每日元数据采集-新");
        assertThat(updated.getSchedule()).isEqualTo("0 0 4 * * ?");
        assertThat(updated.getMode()).isEqualTo("full");
        assertThat(updated.getStrategy()).isEqualTo("overwrite");
        assertThat(updated.getStatus()).isEqualTo("pending");
        assertThat(updated.getFailReason()).isNull();
        CollectorTask persisted = repository.findById(task.getId()).orElseThrow(AssertionError::new);
        assertThat(persisted.getSchedule().getValue()).isEqualTo("0 0 4 * * ?");
    }

    @Test
    @DisplayName("编辑为重名调度（同源+调度冲突，排除自身）抛出冲突")
    void updateDuplicateConnectorAndScheduleRejected() {
        CollectorVO first = appService.create(buildAddCmd("任务一", "c-1", "0 0 2 * * ?"));
        appService.create(buildAddCmd("任务二", "c-1", "0 0 3 * * ?"));

        CollectorUpdateCmd updateCmd = new CollectorUpdateCmd();
        updateCmd.setId(first.getId());
        updateCmd.setName("任务一");
        updateCmd.setConnectorId("c-1");
        updateCmd.setSchedule("0 0 3 * * ?");
        updateCmd.setMode(CollectorMode.INCREMENTAL);
        updateCmd.setStrategy(CollectorStrategy.IGNORE);
        updateCmd.setAutoClassify(Boolean.TRUE);
        assertThatThrownBy(() -> appService.update(first.getId(), updateCmd))
                .isInstanceOf(CollectorTaskConflictException.class);

        // 保持自身调度不冲突
        updateCmd.setSchedule("0 0 2 * * ?");
        appService.update(first.getId(), updateCmd);
        assertThat(repository.findById(first.getId())).isPresent();
    }

    @Test
    @DisplayName("开始执行：待执行→运行中并持久化")
    void startThenRunning() {
        CollectorVO task = appService.create(buildAddCmd("每日元数据采集", "c-1", "0 0 2 * * ?"));

        CollectorVO started = appService.start(task.getId());

        assertThat(started.getStatus()).isEqualTo("running");
        assertThat(repository.findById(task.getId()).orElseThrow(AssertionError::new).getStatus()).isEqualTo(CollectorTaskStatus.RUNNING);
    }

    @Test
    @DisplayName("运行中再次开始执行被拒绝（幂等）")
    void startWhileRunningRejected() {
        CollectorVO task = appService.create(buildAddCmd("每日元数据采集", "c-1", "0 0 2 * * ?"));
        appService.start(task.getId());

        assertThatThrownBy(() -> appService.start(task.getId()))
                .isInstanceOf(CollectorTaskStateConflictException.class)
                .hasMessageContaining("不可重复触发");
    }

    @Test
    @DisplayName("失败任务可重新开始执行（失败重跑）")
    void startAfterFailureAllowed() {
        CollectorVO task = appService.create(buildAddCmd("每日元数据采集", "c-1", "0 0 2 * * ?"));
        appService.start(task.getId());
        appService.markFailed(task.getId(), "连接超时");

        CollectorVO restarted = appService.start(task.getId());

        assertThat(restarted.getStatus()).isEqualTo("running");
        assertThat(restarted.getFailReason()).isNull();
    }

    @Test
    @DisplayName("取消运行中任务：运行中→已取消并持久化")
    void cancelRunningThenCancelled() {
        CollectorVO task = appService.create(buildAddCmd("每日元数据采集", "c-1", "0 0 2 * * ?"));
        appService.start(task.getId());

        CollectorVO cancelled = appService.cancel(task.getId());

        assertThat(cancelled.getStatus()).isEqualTo("cancelled");
        assertThat(repository.findById(task.getId()).orElseThrow(AssertionError::new).getStatus()).isEqualTo(CollectorTaskStatus.CANCELLED);
    }

    @Test
    @DisplayName("取消待执行任务被拒绝（取消仅运行中，409 语义）")
    void cancelPendingRejected() {
        CollectorVO task = appService.create(buildAddCmd("每日元数据采集", "c-1", "0 0 2 * * ?"));

        assertThatThrownBy(() -> appService.cancel(task.getId()))
                .isInstanceOf(CollectorTaskStateConflictException.class)
                .hasMessageContaining("仅运行中");
    }

    @Test
    @DisplayName("标记成功：运行中→成功")
    void markSucceededThenSuccess() {
        CollectorVO task = appService.create(buildAddCmd("每日元数据采集", "c-1", "0 0 2 * * ?"));
        appService.start(task.getId());

        CollectorVO done = appService.markSucceeded(task.getId());

        assertThat(done.getStatus()).isEqualTo("success");
        assertThat(repository.findById(task.getId()).orElseThrow(AssertionError::new).getStatus()).isEqualTo(CollectorTaskStatus.SUCCESS);
    }

    @Test
    @DisplayName("标记失败：运行中→失败并持久化失败原因（局部重采语义）")
    void markFailedThenFailedWithCause() {
        CollectorVO task = appService.create(buildAddCmd("每日元数据采集", "c-1", "0 0 2 * * ?"));
        appService.start(task.getId());

        CollectorVO failed = appService.markFailed(task.getId(), "连接超时：table scan failed");

        assertThat(failed.getStatus()).isEqualTo("failed");
        assertThat(failed.getFailReason()).isEqualTo("连接超时：table scan failed");
        CollectorTask persisted = repository.findById(task.getId()).orElseThrow(AssertionError::new);
        assertThat(persisted.getStatus()).isEqualTo(CollectorTaskStatus.FAILED);
        assertThat(persisted.getFailReason()).isEqualTo("连接超时：table scan failed");
    }

    @Test
    @DisplayName("根据 ID 获取采集任务详情：存在返回 VO")
    void getByIdReturnsTaskWhenFound() {
        CollectorVO created = appService.create(buildAddCmd("详情测试任务", "c-1", "0 0 2 * * ?"));
        CollectorVO found = appService.getById(created.getId());

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getName()).isEqualTo("详情测试任务");
    }

    @Test
    @DisplayName("根据 ID 获取不存在的采集任务抛出 404")
    void getByIdThrowsWhenNotFound() {
        assertThatThrownBy(() -> appService.getById("non-existent-id"))
                .isInstanceOf(CollectorTaskNotFoundException.class);
    }

    @Test
    @DisplayName("删除未运行采集任务成功从仓储中移除")
    void deleteRemovesTaskWhenNotRunning() {
        CollectorVO created = appService.create(buildAddCmd("待删除任务", "c-1", "0 0 2 * * ?"));
        assertThat(repository.findById(created.getId())).isPresent();

        appService.delete(created.getId());

        assertThat(repository.findById(created.getId())).isEmpty();
    }

    @Test
    @DisplayName("删除运行中采集任务抛出状态冲突（409 语义）")
    void deleteRunningTaskThrowsConflict() {
        CollectorVO created = appService.create(buildAddCmd("运行中任务", "c-1", "0 0 2 * * ?"));
        appService.start(created.getId());

        assertThatThrownBy(() -> appService.delete(created.getId()))
                .isInstanceOf(CollectorTaskStateConflictException.class)
                .hasMessageContaining("运行中的采集任务不能删除");
        assertThat(repository.findById(created.getId())).isPresent();
    }

    @Test
    @DisplayName("开始/取消/标记/编辑/删除不存在的任务抛出未找到（404 语义）")
    void operationsOnNotFoundThrows() {
        assertThatThrownBy(() -> appService.start("not-exist"))
                .isInstanceOf(CollectorTaskNotFoundException.class);
        assertThatThrownBy(() -> appService.cancel("not-exist"))
                .isInstanceOf(CollectorTaskNotFoundException.class);
        assertThatThrownBy(() -> appService.markSucceeded("not-exist"))
                .isInstanceOf(CollectorTaskNotFoundException.class);
        assertThatThrownBy(() -> appService.markFailed("not-exist", "cause"))
                .isInstanceOf(CollectorTaskNotFoundException.class);
        assertThatThrownBy(() -> appService.delete("not-exist"))
                .isInstanceOf(CollectorTaskNotFoundException.class);
        CollectorUpdateCmd updateCmd = new CollectorUpdateCmd();
        updateCmd.setId("not-exist");
        assertThatThrownBy(() -> appService.update("not-exist", updateCmd))
                .isInstanceOf(CollectorTaskNotFoundException.class);
    }

    private CollectorAddCmd buildAddCmd(String name, String connectorId, String schedule) {
        CollectorAddCmd cmd = new CollectorAddCmd();
        cmd.setName(name);
        cmd.setConnectorId(connectorId);
        cmd.setSchedule(schedule);
        cmd.setMode(CollectorMode.INCREMENTAL);
        cmd.setStrategy(CollectorStrategy.IGNORE);
        cmd.setAutoClassify(Boolean.TRUE);
        return cmd;
    }
}
