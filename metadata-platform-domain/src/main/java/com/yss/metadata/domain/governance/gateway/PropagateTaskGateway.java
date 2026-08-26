package com.yss.metadata.domain.governance.gateway;

import com.yss.metadata.domain.governance.model.PropagateTask;

import java.util.Optional;

/**
 * 分类传播任务仓储端口（治理域；Domain 定义，Infrastructure 实现）。
 *
 * <p>propagate_task 表：同 classification+version 任务查询（幂等复用判定）与保存（含状态流转）。</p>
 */
public interface PropagateTaskGateway {

    /**
     * 查询同分类同版本的既有任务（任意状态；同版本只跑一次的幂等依据）。
     */
    Optional<PropagateTask> findByClassificationAndVersion(String classificationId, String version);

    /**
     * 保存传播任务（新增或更新状态）。
     */
    PropagateTask save(PropagateTask task);
}
