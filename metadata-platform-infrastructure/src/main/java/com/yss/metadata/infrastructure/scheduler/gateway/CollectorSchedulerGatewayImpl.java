package com.yss.metadata.infrastructure.scheduler.gateway;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.ExecutionComplete;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import com.github.kagkarlsson.scheduler.task.helper.PlainScheduleAndData;
import com.github.kagkarlsson.scheduler.task.schedule.CronSchedule;
import com.yss.metadata.domain.collector.gateway.CollectorSchedulerGateway;
import com.yss.metadata.domain.collector.model.CollectorTask;
import com.yss.metadata.infrastructure.scheduler.config.DbSchedulerProperties;
import com.yss.metadata.infrastructure.scheduler.task.MetadataCollectorTaskDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

/**
 * 采集任务调度网关实现（基于 db-scheduler）。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CollectorSchedulerGatewayImpl implements CollectorSchedulerGateway {

    private final ObjectProvider<Scheduler> schedulerProvider;
    private final MetadataCollectorTaskDefinition taskDefinition;
    private final DbSchedulerProperties properties;

    @Override
    public void syncSchedule(CollectorTask task) {
        if (task == null || task.getId() == null) {
            return;
        }
        Scheduler scheduler = schedulerProvider.getIfAvailable();
        if (scheduler == null) {
            log.debug("db-scheduler 未启用或不可用，跳过调度同步, taskId={}", task.getId());
            return;
        }

        if (Boolean.FALSE.equals(task.getEnabled())) {
            cancelSchedule(task.getId());
            return;
        }

        String scheduleVal = task.getSchedule() != null ? task.getSchedule().getValue() : null;
        if (scheduleVal == null || scheduleVal.trim().isEmpty()) {
            log.warn("采集任务未配置有效调度表达式，跳过排程: taskId={}", task.getId());
            cancelSchedule(task.getId());
            return;
        }

        String trimmed = scheduleVal.trim();
        if ("manual".equalsIgnoreCase(trimmed) || "once".equalsIgnoreCase(trimmed) || "none".equalsIgnoreCase(trimmed)) {
            log.debug("采集任务为手动触发模式，无需定时排程: taskId={}, schedule={}", task.getId(), trimmed);
            cancelSchedule(task.getId());
            return;
        }

        String cronPattern = normalizeCronPattern(trimmed);
        if (!isValidCronPattern(cronPattern)) {
            log.warn("采集任务调度表达式非有效 Cron 格式，跳过排程: taskId={}, schedule={}", task.getId(), trimmed);
            cancelSchedule(task.getId());
            return;
        }

        try {
            ZoneId zoneId = ZoneId.of(properties.getZoneId());
            CronSchedule cronSchedule = new CronSchedule(cronPattern, zoneId);
            PlainScheduleAndData data = new PlainScheduleAndData(cronSchedule, task.getId());

            Instant nextExecution = cronSchedule.getNextExecutionTime(ExecutionComplete.simulatedSuccess(Instant.now()));
            if (nextExecution == null) {
                log.warn("计算下次执行时间为空，无法排程: taskId={}, cron={}", task.getId(), cronPattern);
                cancelSchedule(task.getId());
                return;
            }

            TaskInstance<PlainScheduleAndData> instance = taskDefinition.getTask().instance(task.getId(), data);
            boolean rescheduled = scheduler.reschedule(instance, nextExecution);
            if (!rescheduled) {
                scheduler.schedule(instance, nextExecution);
            }
            log.info("同步采集任务调度成功: taskId={}, cron={}, nextExecution={}", task.getId(), cronPattern, nextExecution);
        } catch (IllegalArgumentException e) {
            log.warn("采集任务 Cron 表达式格式不合法或不被支持，跳过排程: taskId={}, cron={}, reason={}", task.getId(), cronPattern, e.getMessage());
            cancelSchedule(task.getId());
        } catch (Exception e) {
            log.error("同步采集任务调度失败: taskId={}, cron={}", task.getId(), cronPattern, e);
        }
    }

    @Override
    public void cancelSchedule(String taskId) {
        if (taskId == null) {
            return;
        }
        Scheduler scheduler = schedulerProvider.getIfAvailable();
        if (scheduler == null) {
            return;
        }
        try {
            TaskInstanceId instanceId = TaskInstanceId.of(MetadataCollectorTaskDefinition.TASK_NAME, taskId);
            scheduler.cancel(instanceId);
            log.info("已取消采集任务调度: taskId={}", taskId);
        } catch (Exception e) {
            log.warn("取消采集任务调度异常: taskId={}, message={}", taskId, e.getMessage());
        }
    }

    @Override
    public void triggerNow(String taskId) {
        if (taskId == null) {
            return;
        }
        Scheduler scheduler = schedulerProvider.getIfAvailable();
        if (scheduler == null) {
            log.warn("db-scheduler 未启用，无法触发立即执行, taskId={}", taskId);
            return;
        }
        try {
            TaskInstanceId instanceId = TaskInstanceId.of(MetadataCollectorTaskDefinition.TASK_NAME, taskId);
            scheduler.reschedule(instanceId, Instant.now());
            scheduler.triggerCheckForDueExecutions();
            log.info("已触发采集任务立即调度执行: taskId={}", taskId);
        } catch (Exception e) {
            log.error("触发采集任务立即调度执行失败: taskId={}", taskId, e);
        }
    }

    @Override
    public Optional<Instant> getNextExecutionTime(String taskId) {
        if (taskId == null) {
            return Optional.empty();
        }
        Scheduler scheduler = schedulerProvider.getIfAvailable();
        if (scheduler == null) {
            return Optional.empty();
        }
        try {
            TaskInstanceId instanceId = TaskInstanceId.of(MetadataCollectorTaskDefinition.TASK_NAME, taskId);
            return scheduler.getScheduledExecution(instanceId)
                    .map(exec -> exec.getExecutionTime());
        } catch (Exception e) {
            log.warn("获取采集任务下次执行时间异常: taskId={}, message={}", taskId, e.getMessage());
            return Optional.empty();
        }
    }

    private String normalizeCronPattern(String cron) {
        if (cron == null) {
            return "";
        }
        String trimmed = cron.trim();
        // 若为标准的 5 段式 cron (分 时 日 月 周)，补齐秒位 '0' 适配 6 段式
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 5) {
            return "0 " + trimmed;
        }
        return trimmed;
    }

    private boolean isValidCronPattern(String cronPattern) {
        if (cronPattern == null || cronPattern.trim().isEmpty()) {
            return false;
        }
        String[] parts = cronPattern.trim().split("\\s+");
        return parts.length >= 6 && parts.length <= 7;
    }
}
