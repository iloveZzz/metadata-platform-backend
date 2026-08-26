package com.yss.metadata.domain.collector;

import com.yss.metadata.domain.collector.exception.CollectorTaskStateConflictException;
import com.yss.metadata.domain.collector.model.CollectSchedule;
import com.yss.metadata.domain.collector.model.CollectorMode;
import com.yss.metadata.domain.collector.model.CollectorStrategy;
import com.yss.metadata.domain.collector.model.CollectorTask;
import com.yss.metadata.domain.collector.model.CollectorTaskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 采集任务状态机行为测试（WU-01-02/03，spec FR-005 / 系统概要设计 §5）。
 *
 * <p>状态机：待执行 → 运行中 → 成功 / 失败 / 已取消。
 * 主控语义纠偏：待执行/成功/失败/已取消均可开始执行（可重新执行），
 * 仅运行中重复触发幂等拒绝；取消仅运行中（409 语义）。</p>
 */
class CollectorTaskTest {

    @Test
    @DisplayName("创建后状态为待执行")
    void createThenStatusIsPending() {
        CollectorTask task = buildTask();

        assertThat(task.getStatus()).isEqualTo(CollectorTaskStatus.PENDING);
        assertThat(task.getFailReason()).isNull();
        assertThat(task.getCreatedAt()).isNotNull();
        assertThat(task.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("待执行开始执行后进入运行中并记录最近执行时间")
    void startFromPendingThenRunning() {
        CollectorTask task = buildTask();
        LocalDateTime beforeStart = task.getUpdatedAt();

        task.start();

        assertThat(task.getStatus()).isEqualTo(CollectorTaskStatus.RUNNING);
        assertThat(task.getLastRunAt()).isNotNull();
        assertThat(task.getUpdatedAt()).isAfterOrEqualTo(beforeStart);
    }

    @Test
    @DisplayName("运行中再次开始执行被拒绝（幂等，状态不改变）")
    void startWhileRunningRejected() {
        CollectorTask task = buildTask();
        task.start();

        assertThatThrownBy(task::start)
                .isInstanceOf(CollectorTaskStateConflictException.class)
                .hasMessageContaining("不可重复触发");
        assertThat(task.getStatus()).isEqualTo(CollectorTaskStatus.RUNNING);
    }

    @Test
    @DisplayName("成功状态可重新开始执行（重新执行语义）")
    void startFromSuccessRestarts() {
        CollectorTask task = buildTask();
        task.start();
        task.markSucceeded();

        task.start();

        assertThat(task.getStatus()).isEqualTo(CollectorTaskStatus.RUNNING);
    }

    @Test
    @DisplayName("失败状态可重新开始执行（失败重跑语义）")
    void startFromFailedRestarts() {
        CollectorTask task = buildTask();
        task.start();
        task.markFailed("连接超时");

        task.start();

        assertThat(task.getStatus()).isEqualTo(CollectorTaskStatus.RUNNING);
        assertThat(task.getFailReason()).isNull();
    }

    @Test
    @DisplayName("已取消可重新开始执行（取消后可重新执行）")
    void startFromCancelledRestarts() {
        CollectorTask task = buildTask();
        task.start();
        task.cancel();

        task.start();

        assertThat(task.getStatus()).isEqualTo(CollectorTaskStatus.RUNNING);
    }

    @Test
    @DisplayName("新一轮开始执行清空旧失败原因")
    void startClearsFailReason() {
        CollectorTask task = buildTask();
        task.start();
        task.markFailed("table scan failed");
        assertThat(task.getFailReason()).isEqualTo("table scan failed");

        task.start();

        assertThat(task.getFailReason()).isNull();
        assertThat(task.getStatus()).isEqualTo(CollectorTaskStatus.RUNNING);
    }

    @Test
    @DisplayName("运行中取消后进入已取消")
    void cancelWhileRunningThenCancelled() {
        CollectorTask task = buildTask();
        task.start();

        task.cancel();

        assertThat(task.getStatus()).isEqualTo(CollectorTaskStatus.CANCELLED);
    }

    @Test
    @DisplayName("待执行状态取消被拒绝（取消仅运行中）")
    void cancelWhilePendingRejected() {
        CollectorTask task = buildTask();

        assertThatThrownBy(task::cancel)
                .isInstanceOf(CollectorTaskStateConflictException.class)
                .hasMessageContaining("仅运行中");
        assertThat(task.getStatus()).isEqualTo(CollectorTaskStatus.PENDING);
    }

    @Test
    @DisplayName("成功状态取消被拒绝（取消仅运行中）")
    void cancelWhileSuccessRejected() {
        CollectorTask task = buildTask();
        task.start();
        task.markSucceeded();

        assertThatThrownBy(task::cancel)
                .isInstanceOf(CollectorTaskStateConflictException.class)
                .hasMessageContaining("仅运行中");
    }

    @Test
    @DisplayName("失败状态取消被拒绝（取消仅运行中）")
    void cancelWhileFailedRejected() {
        CollectorTask task = buildTask();
        task.start();
        task.markFailed("连接超时");

        assertThatThrownBy(task::cancel)
                .isInstanceOf(CollectorTaskStateConflictException.class)
                .hasMessageContaining("仅运行中");
    }

    @Test
    @DisplayName("已取消状态再次取消被拒绝")
    void cancelWhileCancelledRejected() {
        CollectorTask task = buildTask();
        task.start();
        task.cancel();

        assertThatThrownBy(task::cancel)
                .isInstanceOf(CollectorTaskStateConflictException.class)
                .hasMessageContaining("仅运行中");
    }

    @Test
    @DisplayName("运行中标记成功后进入成功并清空失败原因")
    void markSucceededFromRunning() {
        CollectorTask task = buildTask();
        task.start();

        task.markSucceeded();

        assertThat(task.getStatus()).isEqualTo(CollectorTaskStatus.SUCCESS);
        assertThat(task.getFailReason()).isNull();
    }

    @Test
    @DisplayName("非运行中标记成功被拒绝")
    void markSucceededFromPendingRejected() {
        CollectorTask task = buildTask();

        assertThatThrownBy(task::markSucceeded)
                .isInstanceOf(CollectorTaskStateConflictException.class)
                .hasMessageContaining("仅允许运行中");
    }

    @Test
    @DisplayName("运行中标记失败进入失败并携带失败原因（局部重采语义字段）")
    void markFailedFromRunningWithCause() {
        CollectorTask task = buildTask();
        task.start();

        task.markFailed("连接超时：table scan failed");

        assertThat(task.getStatus()).isEqualTo(CollectorTaskStatus.FAILED);
        assertThat(task.getFailReason()).isEqualTo("连接超时：table scan failed");
    }

    @Test
    @DisplayName("非运行中标记失败被拒绝")
    void markFailedFromPendingRejected() {
        CollectorTask task = buildTask();

        assertThatThrownBy(() -> task.markFailed("cause"))
                .isInstanceOf(CollectorTaskStateConflictException.class)
                .hasMessageContaining("仅允许运行中");
    }

    @Test
    @DisplayName("配置变更后状态重置为待执行并清空失败原因")
    void updateResetsStatusToPending() {
        CollectorTask task = buildTask();
        task.start();
        task.markFailed("连接超时");

        task.update("新任务名", "c-2", new CollectSchedule("0 0 3 * * ?"), CollectorMode.FULL,
                CollectorStrategy.OVERWRITE, Boolean.FALSE);

        assertThat(task.getStatus()).isEqualTo(CollectorTaskStatus.PENDING);
        assertThat(task.getName()).isEqualTo("新任务名");
        assertThat(task.getConnectorId()).isEqualTo("c-2");
        assertThat(task.getMode()).isEqualTo(CollectorMode.FULL);
        assertThat(task.getStrategy()).isEqualTo(CollectorStrategy.OVERWRITE);
        assertThat(task.getAutoClassify()).isFalse();
        assertThat(task.getFailReason()).isNull();
    }

    @Test
    @DisplayName("不变量校验：任务名称不能为空")
    void validateRejectsBlankName() {
        CollectorTask task = buildTask();

        task.setName("  ");
        assertThatThrownBy(task::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("任务名称");
    }

    @Test
    @DisplayName("不变量校验：目标数据源不能为空")
    void validateRejectsBlankConnectorId() {
        CollectorTask task = buildTask();

        task.setConnectorId(null);
        assertThatThrownBy(task::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("目标数据源");
    }

    @Test
    @DisplayName("不变量校验：调度/采集模式/覆盖策略不能为空")
    void validateRejectsNullScheduleModeStrategy() {
        CollectorTask task = buildTask();

        task.setSchedule(null);
        assertThatThrownBy(task::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("调度");

        task.setSchedule(new CollectSchedule("0 0 2 * * ?"));
        task.setMode(null);
        assertThatThrownBy(task::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("采集模式");

        task.setMode(CollectorMode.FULL);
        task.setStrategy(null);
        assertThatThrownBy(task::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("覆盖策略");
    }

    @Test
    @DisplayName("调度值对象：表达式非空且按值相等")
    void scheduleValueObjectContract() {
        assertThatThrownBy(() -> new CollectSchedule(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("调度表达式");

        assertThat(new CollectSchedule("0 0 2 * * ?"))
                .isEqualTo(new CollectSchedule("0 0 2 * * ?"));
        assertThat(new CollectSchedule("0 0 2 * * ?").getValue()).isEqualTo("0 0 2 * * ?");
    }

    private CollectorTask buildTask() {
        return CollectorTask.builder()
                .id("ct-1")
                .name("每日元数据采集")
                .connectorId("c-1")
                .schedule(new CollectSchedule("0 0 2 * * ?"))
                .mode(CollectorMode.INCREMENTAL)
                .strategy(CollectorStrategy.IGNORE)
                .autoClassify(Boolean.TRUE)
                .status(CollectorTaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
