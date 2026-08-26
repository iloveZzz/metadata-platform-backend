package com.yss.metadata.infrastructure.scheduler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * db-scheduler 调度器配置属性。
 */
@Data
@ConfigurationProperties(prefix = "yss.db-scheduler")
public class DbSchedulerProperties {

    /**
     * 是否开启 db-scheduler 调度器（默认 true）
     */
    private boolean enabled = true;

    /**
     * 轮询数据库到期任务的间隔时间（毫秒，默认 5000ms）
     */
    private long pollingIntervalMs = 5000L;

    /**
     * 调度节点心跳上报间隔时间（毫秒，默认 10000ms）
     */
    private long heartbeatIntervalMs = 10000L;

    /**
     * 调度器执行线程池大小（默认 10）
     */
    private int threads = 10;

    /**
     * 调度队列表名（默认 scheduled_tasks）
     */
    private String tableName = "scheduled_tasks";

    /**
     * 默认时区（默认 Asia/Shanghai）
     */
    private String zoneId = "Asia/Shanghai";
}
