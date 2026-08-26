package com.yss.metadata.infrastructure.scheduler.task;

import com.github.kagkarlsson.scheduler.task.helper.PlainScheduleAndData;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.yss.metadata.domain.collector.exception.CollectorTaskStateConflictException;
import com.yss.metadata.domain.collector.spi.CollectorTaskTriggerSpi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MetadataCollectorTaskDefinitionTest {

    @Mock
    private ObjectProvider<CollectorTaskTriggerSpi> triggerSpiProvider;

    @Mock
    private CollectorTaskTriggerSpi triggerSpi;

    @Test
    @DisplayName("验证 Task 实例化与基本元信息")
    void testTaskDefinitionBasics() {
        when(triggerSpiProvider.getIfAvailable()).thenReturn(triggerSpi);
        MetadataCollectorTaskDefinition taskDef = new MetadataCollectorTaskDefinition(triggerSpiProvider);

        assertThat(taskDef.getTask()).isNotNull();
        assertThat(taskDef.getTask().getName()).isEqualTo(MetadataCollectorTaskDefinition.TASK_NAME);
    }

    @Test
    @DisplayName("验证调度执行正常回调 TriggerSpi")
    void testTaskExecutionSuccess() {
        when(triggerSpiProvider.getIfAvailable()).thenReturn(triggerSpi);
        MetadataCollectorTaskDefinition taskDef = new MetadataCollectorTaskDefinition(triggerSpiProvider);

        PlainScheduleAndData data = new PlainScheduleAndData(FixedDelay.of(Duration.ofMinutes(1)), "task-101");
        TaskInstance<PlainScheduleAndData> instance = taskDef.getTask().instance("task-101", data);

        assertDoesNotThrow(() -> taskDef.getTask().execute(instance, null));
        verify(triggerSpi).execute("task-101");
    }

    @Test
    @DisplayName("验证状态冲突（任务运行中）时安全跳过本轮调度，不抛出异常")
    void testTaskExecutionStateConflictIgnoredSafely() {
        when(triggerSpiProvider.getIfAvailable()).thenReturn(triggerSpi);
        doThrow(new CollectorTaskStateConflictException("采集任务当前处于运行中状态，无法重复触发"))
                .when(triggerSpi).execute("task-102");

        MetadataCollectorTaskDefinition taskDef = new MetadataCollectorTaskDefinition(triggerSpiProvider);

        PlainScheduleAndData data = new PlainScheduleAndData(FixedDelay.of(Duration.ofMinutes(1)), "task-102");
        TaskInstance<PlainScheduleAndData> instance = taskDef.getTask().instance("task-102", data);

        assertDoesNotThrow(() -> taskDef.getTask().execute(instance, null));
        verify(triggerSpi).execute("task-102");
    }

    @Test
    @DisplayName("验证其他异常正常向上传播给 db-scheduler 进行重试与审计记录")
    void testTaskExecutionOtherExceptionPropagated() {
        when(triggerSpiProvider.getIfAvailable()).thenReturn(triggerSpi);
        doThrow(new RuntimeException("数据库连接中断"))
                .when(triggerSpi).execute("task-103");

        MetadataCollectorTaskDefinition taskDef = new MetadataCollectorTaskDefinition(triggerSpiProvider);

        PlainScheduleAndData data = new PlainScheduleAndData(FixedDelay.of(Duration.ofMinutes(1)), "task-103");
        TaskInstance<PlainScheduleAndData> instance = taskDef.getTask().instance("task-103", data);

        assertThrows(RuntimeException.class, () -> taskDef.getTask().execute(instance, null));
        verify(triggerSpi).execute("task-103");
    }
}
