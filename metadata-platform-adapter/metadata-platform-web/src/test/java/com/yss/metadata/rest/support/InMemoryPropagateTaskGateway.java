package com.yss.metadata.rest.support;

import com.yss.metadata.domain.governance.gateway.PropagateTaskGateway;
import com.yss.metadata.domain.governance.model.PropagateTask;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 分类传播任务仓储内存实现（Web 契约测试 seam，与 application 测试替身一致）。
 */
public class InMemoryPropagateTaskGateway implements PropagateTaskGateway {

    private final Map<String, PropagateTask> store = new LinkedHashMap<>();

    public void seed(PropagateTask task) {
        store.put(task.getId(), task);
    }

    @Override
    public Optional<PropagateTask> findByClassificationAndVersion(String classificationId, String version) {
        for (PropagateTask task : store.values()) {
            if (classificationId.equals(task.getClassificationId())
                    && Objects.equals(version, task.getVersion())) {
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
