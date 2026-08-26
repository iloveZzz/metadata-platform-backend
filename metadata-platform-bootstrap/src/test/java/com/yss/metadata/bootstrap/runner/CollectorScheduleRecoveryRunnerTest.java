package com.yss.metadata.bootstrap.runner;

import com.yss.metadata.domain.collector.gateway.CollectorSchedulerGateway;
import com.yss.metadata.domain.collector.gateway.CollectorTaskGateway;
import com.yss.metadata.domain.collector.model.CollectSchedule;
import com.yss.metadata.domain.collector.model.CollectorTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectorScheduleRecoveryRunnerTest {

    @Mock
    private CollectorTaskGateway collectorTaskGateway;

    @Mock
    private CollectorSchedulerGateway collectorSchedulerGateway;

    @Test
    @DisplayName("启动自愈引导：正确处理定时任务与 manual 任务，不因非法表达式中断启动")
    void testRecoveryRunner() {
        CollectorTask task1 = new CollectorTask();
        task1.setId("task-cron");
        task1.setEnabled(true);
        task1.setSchedule(new CollectSchedule("0 0 2 * * ?"));

        CollectorTask task2 = new CollectorTask();
        task2.setId("task-manual");
        task2.setEnabled(true);
        task2.setSchedule(new CollectSchedule("manual"));

        CollectorTask task3 = new CollectorTask();
        task3.setId("task-disabled");
        task3.setEnabled(false);
        task3.setSchedule(new CollectSchedule("0 0 3 * * ?"));

        when(collectorTaskGateway.findAll()).thenReturn(Arrays.asList(task1, task2, task3));

        CollectorScheduleRecoveryRunner runner = new CollectorScheduleRecoveryRunner(collectorTaskGateway, collectorSchedulerGateway);
        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(collectorSchedulerGateway, times(1)).syncSchedule(task1);
        verify(collectorSchedulerGateway, times(1)).syncSchedule(task2);
        verify(collectorSchedulerGateway, never()).syncSchedule(task3);
    }
}
