package com.yss.metadata.domain.collector.spi;

/**
 * 采集任务调度触发执行端口（Domain 定义，Application 实现，Infrastructure 调度器回调）。
 *
 * <p>遵循依赖倒置原则（DIP），使基础设施层调度器（db-scheduler）仅依赖领域层端口，
 * 与应用层具体编排服务解耦。</p>
 */
@FunctionalInterface
public interface CollectorTaskTriggerSpi {

    /**
     * 触发指定采集任务执行。
     *
     * @param collectorTaskId 采集任务 ID
     */
    void execute(String collectorTaskId);
}
