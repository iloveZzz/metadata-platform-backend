package com.yss.metadata.infrastructure.scheduler.gateway;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import com.github.kagkarlsson.scheduler.task.helper.PlainScheduleAndData;
import com.github.kagkarlsson.scheduler.task.schedule.CronSchedule;
import com.yss.metadata.domain.collector.model.CollectSchedule;
import com.yss.metadata.domain.collector.model.CollectorTask;
import com.yss.metadata.domain.collector.spi.CollectorTaskTriggerSpi;
import com.yss.metadata.infrastructure.scheduler.config.DbSchedulerProperties;
import com.yss.metadata.infrastructure.scheduler.task.MetadataCollectorTaskDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CollectorSchedulerGatewayImplTest {

    @Mock
    private Scheduler scheduler;

    @Mock
    private ObjectProvider<Scheduler> schedulerProvider;

    @Mock
    private ObjectProvider<CollectorTaskTriggerSpi> triggerSpiProvider;

    private MetadataCollectorTaskDefinition taskDefinition;
    private DbSchedulerProperties properties;
    private CollectorSchedulerGatewayImpl gateway;

    @BeforeEach
    void setUp() {
        when(schedulerProvider.getIfAvailable()).thenReturn(scheduler);
        taskDefinition = new MetadataCollectorTaskDefinition(triggerSpiProvider);
        properties = new DbSchedulerProperties();
        properties.setZoneId("Asia/Shanghai");
        properties.setTableName("scheduled_tasks");
        gateway = new CollectorSchedulerGatewayImpl(schedulerProvider, taskDefinition, properties);
    }

    @Test
    @DisplayName("测试同步调度：enabled=true 且为 5 段式 cron，正常排程")
    void testSyncScheduleFivePartCron() {
        CollectorTask task = new CollectorTask();
        task.setId("task-001");
        task.setEnabled(true);
        task.setSchedule(new CollectSchedule("0 2 * * *")); // 每天凌晨2点

        when(scheduler.reschedule(any(), any())).thenReturn(false);

        gateway.syncSchedule(task);

        verify(scheduler).reschedule(any(TaskInstance.class), any(Instant.class));
        verify(scheduler).schedule(any(TaskInstance.class), any(Instant.class));
    }

    @Test
    @DisplayName("测试同步调度：enabled=true 且为 6 段式 cron，正常排程")
    void testSyncScheduleSixPartCron() {
        CollectorTask task = new CollectorTask();
        task.setId("task-002");
        task.setEnabled(true);
        task.setSchedule(new CollectSchedule("0 0 2 * * ?"));

        when(scheduler.reschedule(any(TaskInstance.class), any(Instant.class))).thenReturn(true);

        gateway.syncSchedule(task);

        verify(scheduler).reschedule(any(TaskInstance.class), any(Instant.class));
        verify(scheduler, never()).schedule(any(TaskInstance.class), any(Instant.class));
    }

    @Test
    @DisplayName("测试同步调度：enabled=false，取消排程")
    void testSyncScheduleDisabled() {
        CollectorTask task = new CollectorTask();
        task.setId("task-003");
        task.setEnabled(false);
        task.setSchedule(new CollectSchedule("0 2 * * *"));

        gateway.syncSchedule(task);

        verify(scheduler).cancel(eq(TaskInstanceId.of(MetadataCollectorTaskDefinition.TASK_NAME, "task-003")));
    }

    @Test
    @DisplayName("测试取消调度")
    void testCancelSchedule() {
        gateway.cancelSchedule("task-004");
        verify(scheduler).cancel(eq(TaskInstanceId.of(MetadataCollectorTaskDefinition.TASK_NAME, "task-004")));
    }

    @Test
    @DisplayName("测试立即触发调度")
    void testTriggerNow() {
        gateway.triggerNow("task-005");
        verify(scheduler).reschedule(eq(TaskInstanceId.of(MetadataCollectorTaskDefinition.TASK_NAME, "task-005")), any(Instant.class));
        verify(scheduler).triggerCheckForDueExecutions();
    }

    @Test
    @DisplayName("测试同步调度：enabled=true 但为 manual 手动模式，取消/不创建定时排程")
    void testSyncScheduleManual() {
        CollectorTask task = new CollectorTask();
        task.setId("task-manual");
        task.setEnabled(true);
        task.setSchedule(new CollectSchedule("manual"));

        gateway.syncSchedule(task);

        verify(scheduler).cancel(eq(TaskInstanceId.of(MetadataCollectorTaskDefinition.TASK_NAME, "task-manual")));
        verify(scheduler, never()).schedule(any(TaskInstance.class), any(Instant.class));
        verify(scheduler, never()).reschedule(any(TaskInstance.class), any(Instant.class));
    }

    @Test
    @DisplayName("测试同步调度：enabled=true 但为 once 单次模式，取消/不创建定时排程")
    void testSyncScheduleOnce() {
        CollectorTask task = new CollectorTask();
        task.setId("task-once");
        task.setEnabled(true);
        task.setSchedule(new CollectSchedule("once"));

        gateway.syncSchedule(task);

        verify(scheduler).cancel(eq(TaskInstanceId.of(MetadataCollectorTaskDefinition.TASK_NAME, "task-once")));
        verify(scheduler, never()).schedule(any(TaskInstance.class), any(Instant.class));
    }

    @Test
    @DisplayName("测试同步调度：非法单字段表达式或非 Cron 格式，安全跳过且不抛异常")
    void testSyncScheduleInvalidPattern() {
        CollectorTask task = new CollectorTask();
        task.setId("task-invalid");
        task.setEnabled(true);
        task.setSchedule(new CollectSchedule("not_a_cron_expression"));

        gateway.syncSchedule(task);

        verify(scheduler).cancel(eq(TaskInstanceId.of(MetadataCollectorTaskDefinition.TASK_NAME, "task-invalid")));
        verify(scheduler, never()).schedule(any(TaskInstance.class), any(Instant.class));
    }

    @Test
    @DisplayName("测试 CronSchedule 支持的表达式格式")
    void testCronScheduleFormats() {
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
        // 6 段式 cron
        CronSchedule cron6 = new CronSchedule("0 0 2 * * ?", zoneId);
        assertThat(cron6).isNotNull();

        // 6 段式包含秒
        CronSchedule cron6Sec = new CronSchedule("*/10 * * * * ?", zoneId);
        assertThat(cron6Sec).isNotNull();
    }
}
