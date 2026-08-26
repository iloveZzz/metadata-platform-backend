package com.yss.metadata.infrastructure.scheduler.config;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.Task;
import com.yss.metadata.infrastructure.scheduler.task.MetadataCollectorTaskDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * db-scheduler 调度器 Spring 自动装配配置。
 */
@Configuration
@EnableConfigurationProperties(DbSchedulerProperties.class)
@ConditionalOnProperty(prefix = "yss.db-scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class DbSchedulerConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Scheduler scheduler(DataSource dataSource,
                               MetadataCollectorTaskDefinition metadataCollectorTaskDefinition,
                               DbSchedulerProperties properties) {
        log.info("初始化 db-scheduler 调度器: tableName={}, threads={}, pollingIntervalMs={}, heartbeatIntervalMs={}",
                properties.getTableName(), properties.getThreads(), properties.getPollingIntervalMs(), properties.getHeartbeatIntervalMs());

        List<Task<?>> tasks = Collections.singletonList(metadataCollectorTaskDefinition.getTask());

        return Scheduler.create(dataSource, tasks)
                .tableName(properties.getTableName())
                .threads(properties.getThreads())
                .pollingInterval(Duration.ofMillis(properties.getPollingIntervalMs()))
                .heartbeatInterval(Duration.ofMillis(properties.getHeartbeatIntervalMs()))
                .enableImmediateExecution()
                .build();
    }
}
