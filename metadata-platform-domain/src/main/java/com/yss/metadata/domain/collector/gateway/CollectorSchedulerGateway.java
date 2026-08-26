package com.yss.metadata.domain.collector.gateway;

import com.yss.metadata.domain.collector.model.CollectorTask;

import java.time.Instant;
import java.util.Optional;

/**
 * 采集任务调度网关端口（Domain 定义，Infrastructure 实现）。
 *
 * <p>将采集任务的动态调度、排程更新、取消与立即触发与底层调度器（如 db-scheduler）解耦，
 * 保证领域层纯粹性。</p>
 */
public interface CollectorSchedulerGateway {

    /**
     * 同步/注册采集任务调度。
     *
     * <p>若 task.enabled 为 true 且具备有效调度表达式，则在调度器中注册或更新定时排程；
     * 若 task.enabled 为 false，则从调度器中取消该任务排程。</p>
     *
     * @param task 采集任务聚合根
     */
    void syncSchedule(CollectorTask task);

    /**
     * 取消/移除指定采集任务的定时排程。
     *
     * @param taskId 采集任务 ID
     */
    void cancelSchedule(String taskId);

    /**
     * 触发指定采集任务立即异步执行一次。
     *
     * @param taskId 采集任务 ID
     */
    void triggerNow(String taskId);

    /**
     * 获取指定采集任务在调度器中的下次计划执行时间。
     *
     * @param taskId 采集任务 ID
     * @return 下次计划执行时间（若未排程则返回 empty）
     */
    Optional<Instant> getNextExecutionTime(String taskId);
}
