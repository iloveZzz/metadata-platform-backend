package com.yss.metadata.infrastructure.scheduler.task;

import com.github.kagkarlsson.scheduler.task.helper.PlainScheduleAndData;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTaskWithPersistentSchedule;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.yss.metadata.domain.collector.exception.CollectorTaskStateConflictException;
import com.yss.metadata.domain.collector.spi.CollectorTaskTriggerSpi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 元数据采集任务在 db-scheduler 中的定义与执行处理器。
 *
 * <p>基于 {@link RecurringTaskWithPersistentSchedule}，每个任务实例（task_instance=taskId）
 * 持久化自身独立的调度表达式，在执行完成后自动计算下一次触发时间。</p>
 */
@Component
@Slf4j
public class MetadataCollectorTaskDefinition {

    public static final String TASK_NAME = "METADATA_COLLECTOR";

    private final RecurringTaskWithPersistentSchedule<PlainScheduleAndData> task;

    public MetadataCollectorTaskDefinition(ObjectProvider<CollectorTaskTriggerSpi> triggerSpiProvider) {
        this.task = Tasks.recurringWithPersistentSchedule(TASK_NAME, PlainScheduleAndData.class)
                .onDeadExecutionRevive()
                .execute((taskInstance, executionContext) -> {
                    String taskId = taskInstance.getId();
                    log.info("db-scheduler 触发元数据采集任务调度执行, taskId={}", taskId);
                    try {
                        CollectorTaskTriggerSpi triggerSpi = triggerSpiProvider.getIfAvailable();
                        if (triggerSpi != null) {
                            triggerSpi.execute(taskId);
                        } else {
                            log.warn("未配置可用 CollectorTaskTriggerSpi，跳过调度执行, taskId={}", taskId);
                        }
                        log.info("db-scheduler 元数据采集任务调度执行完成, taskId={}", taskId);
                    } catch (CollectorTaskStateConflictException e) {
                        // 任务可能正在被手动触发运行中，安全跳过本轮调度，避免抛异常导致死锁或重试风暴
                        log.warn("采集任务处于运行中状态，跳过本轮定时调度: taskId={}, message={}", taskId, e.getMessage());
                    } catch (Exception e) {
                        log.error("采集任务定时调度执行异常, taskId={}", taskId, e);
                        throw e; // 抛出异常由 db-scheduler 记录失败并依据策略处理
                    }
                });
    }

    public RecurringTaskWithPersistentSchedule<PlainScheduleAndData> getTask() {
        return task;
    }
}
