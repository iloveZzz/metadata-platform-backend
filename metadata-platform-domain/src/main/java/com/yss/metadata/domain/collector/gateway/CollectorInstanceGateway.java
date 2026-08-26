package com.yss.metadata.domain.collector.gateway;

import com.yss.metadata.domain.collector.model.CollectorInstance;

import java.util.List;
import java.util.Optional;

/**
 * 采集实例仓储网关接口。
 */
public interface CollectorInstanceGateway {

    /**
     * 查询全部采集实例。
     */
    List<CollectorInstance> findAll();

    /**
     * 条件查询采集实例列表。
     */
    List<CollectorInstance> findByQuery(com.yss.metadata.client.dto.query.CollectorInstanceQuery query);

    /**
     * 按 ID 查询采集实例。
     */
    Optional<CollectorInstance> findById(String id);

    /**
     * 按采集任务 ID 查询实例列表。
     */
    List<CollectorInstance> findByCollectorId(String collectorId);

    /**
     * 保存采集实例。
     */
    CollectorInstance save(CollectorInstance instance);

    /**
     * 批量保存采集实例。
     */
    List<CollectorInstance> saveAll(List<CollectorInstance> instances);

    /**
     * 删除采集实例。
     */
    void deleteById(String id);
}
