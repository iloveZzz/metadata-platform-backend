package com.yss.metadata.application.governance.support;

import com.yss.metadata.domain.governance.gateway.PropagateTaskGateway;
import com.yss.metadata.domain.governance.model.PropagateTask;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 分类传播任务仓储内存实现（应用/契约测试 seam）。
 */
public class InMemoryPropagateTaskGateway implements PropagateTaskGateway {

    private final Map<String, PropagateTask> store = new LinkedHashMap<>();

    public void seed(PropagateTask task) {
        store.put(task.getId(), task);
    }

    public Map<String, PropagateTask> store() {
        return store;
    }

    @Override
    public Optional<PropagateTask> findByClassificationAndVersion(String classificationId, String version) {
        for (PropagateTask task : store.values()) {
            if (classificationId.equals(task.getClassificationId())
                    && java.util.Objects.equals(version, task.getVersion())) {
                return Optional.of(task);
            }
        }
        return Optional.empty();
    }

    @Override
    public PropagateTask save(PropagateTask task) {
        store.put(task.getId(), task);
        return task;
    }
}
