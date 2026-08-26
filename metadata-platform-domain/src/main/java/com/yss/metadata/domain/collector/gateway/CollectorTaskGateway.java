package com.yss.metadata.domain.collector.gateway;

import com.yss.metadata.domain.collector.model.CollectSchedule;
import com.yss.metadata.domain.collector.model.CollectorTask;

import java.util.List;
import java.util.Optional;

/**
 * 采集任务仓储端口（Domain 定义，Infrastructure 实现）。
 */
public interface CollectorTaskGateway {

    /**
     * 查询全部采集任务。
     */
    List<CollectorTask> findAll();

    /**
     * 条件查询采集任务列表。
     */
    List<CollectorTask> findByQuery(com.yss.metadata.client.dto.query.CollectorQuery query);

    /**
     * 按 id 查询采集任务。
     */
    Optional<CollectorTask> findById(String id);

    /**
     * 同数据源 + 调度是否已存在（创建幂等唯一性，409 语义）。
     */
    boolean existsByConnectorIdAndSchedule(String connectorId, CollectSchedule schedule);

    /**
     * 排除指定 id 后同数据源 + 调度是否已存在（更新时避免自身冲突）。
     */
    boolean existsByConnectorIdAndScheduleExcluding(String connectorId, CollectSchedule schedule, String excludeId);

    /**
     * 是否存在引用指定数据源（连接器）的采集任务（连接器删除保护，409 语义）。
     */
    boolean existsByConnectorId(String connectorId);

    /**
     * 保存采集任务（新增或更新）。
     */
    CollectorTask save(CollectorTask task);

    /**
     * 按 id 删除采集任务。
     */
    void deleteById(String id);
}
