package com.yss.metadata.application.collector.support;

import com.yss.metadata.domain.collector.model.CollectSchedule;
import com.yss.metadata.domain.collector.model.CollectorTask;
import com.yss.metadata.domain.collector.gateway.CollectorTaskGateway;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 采集任务仓储内存实现（测试 seam，seam-deferred）。
 *
 * <p>仅供单元测试使用，替代真实数据库持久化；
 * 生产持久化实现（MyBatis PO/Mapper + collector_task 迁移脚本）已随 WU-01-03 落地。</p>
 */
public class InMemoryCollectorTaskRepository implements CollectorTaskGateway {

    private final ConcurrentMap<String, CollectorTask> store = new ConcurrentHashMap<>();

    @Override
    public List<CollectorTask> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<CollectorTask> findByQuery(com.yss.metadata.client.dto.query.CollectorQuery query) {
        if (query == null) return findAll();
        return store.values().stream().filter(task -> {
            if (query.getKeyword() != null && !query.getKeyword().trim().isEmpty()) {
                String kw = query.getKeyword().trim();
                boolean matchName = task.getName() != null && task.getName().contains(kw);
                boolean matchConnector = task.getConnectorId() != null && task.getConnectorId().contains(kw);
                if (!matchName && !matchConnector) return false;
            }
            if (query.getOwner() != null && !query.getOwner().trim().isEmpty() && !query.getOwner().equals(task.getOwner())) return false;
            if (query.getEnabled() != null && !query.getEnabled().equals(task.getEnabled())) return false;
            if (query.getDatasourceType() != null && !query.getDatasourceType().trim().isEmpty() && !query.getDatasourceType().equals(task.getDatasourceType())) return false;
            if (query.getMode() != null && !query.getMode().trim().isEmpty() && (task.getMode() == null || !query.getMode().equals(task.getMode().getValue()))) return false;
            if (query.getStatus() != null && !query.getStatus().trim().isEmpty() && (task.getStatus() == null || !query.getStatus().equals(task.getStatus().getValue()))) return false;
            return true;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Optional<CollectorTask> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public boolean existsByConnectorIdAndSchedule(String connectorId, CollectSchedule schedule) {
        return store.values().stream().anyMatch(task ->
                task.getConnectorId().equals(connectorId) && task.getSchedule().equals(schedule));
    }

    @Override
    public boolean existsByConnectorIdAndScheduleExcluding(String connectorId, CollectSchedule schedule,
                                                           String excludeId) {
        return store.values().stream().anyMatch(task ->
                !task.getId().equals(excludeId)
                        && task.getConnectorId().equals(connectorId)
                        && task.getSchedule().equals(schedule));
    }

    @Override
    public boolean existsByConnectorId(String connectorId) {
        return store.values().stream().anyMatch(task -> task.getConnectorId().equals(connectorId));
    }

    @Override
    public CollectorTask save(CollectorTask task) {
        store.put(task.getId(), task);
        return task;
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }
}
