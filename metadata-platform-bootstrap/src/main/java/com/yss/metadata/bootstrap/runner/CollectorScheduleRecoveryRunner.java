package com.yss.metadata.bootstrap.runner;

import com.yss.metadata.domain.collector.gateway.CollectorSchedulerGateway;
import com.yss.metadata.domain.collector.gateway.CollectorTaskGateway;
import com.yss.metadata.domain.collector.model.CollectorTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 采集任务调度存量自愈引导器。
 *
 * <p>在 Spring 容器初始化完成后执行，扫描所有处于生效状态（enabled = true）且具备有效调度的采集任务，
 * 重新注册/排程到 db-scheduler 中，确保服务重启、停机升级后定时调度任务不丢失。</p>
 */
@Component
@Slf4j
public class CollectorScheduleRecoveryRunner implements ApplicationRunner {

    private final CollectorTaskGateway collectorTaskGateway;
    private final CollectorSchedulerGateway collectorSchedulerGateway;

    public CollectorScheduleRecoveryRunner(CollectorTaskGateway collectorTaskGateway,
                                          @Autowired(required = false) CollectorSchedulerGateway collectorSchedulerGateway) {
        this.collectorTaskGateway = collectorTaskGateway;
        this.collectorSchedulerGateway = collectorSchedulerGateway;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (collectorSchedulerGateway == null) {
            log.info("CollectorSchedulerGateway 未装配，跳过采集任务调度存量自愈");
            return;
        }
        log.info("开始执行存量生效采集任务调度自愈注册...");
        try {
            List<CollectorTask> tasks = collectorTaskGateway.findAll();
            int recoveredCount = 0;
            for (CollectorTask task : tasks) {
                if (Boolean.TRUE.equals(task.getEnabled()) && task.getSchedule() != null) {
                    collectorSchedulerGateway.syncSchedule(task);
                    recoveredCount++;
                }
            }
            log.info("存量生效采集任务调度自愈完成，已同步注册任务数: {}", recoveredCount);
        } catch (Exception e) {
            log.error("存量生效采集任务调度自愈异常", e);
        }
    }
}
