package com.yss.metadata.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.metadata.domain.governance.gateway.PropagateTaskGateway;
import com.yss.metadata.domain.governance.model.PropagateTask;
import com.yss.metadata.repository.PropagateTaskRepository;
import com.yss.metadata.infrastructure.convertor.PropagateTaskConvertor;
import com.yss.metadata.repository.entity.PropagateTaskPO;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 分类传播任务仓储实现（MyBatis-Plus；propagate_task 表）。
 *
 * <p>幂等复用判定：同 classification_id+version 任务（任意状态）返回；
 * 状态流转经 save 持久化。</p>
 */
@Repository
public class PropagateTaskGatewayImpl implements PropagateTaskGateway {

    private final PropagateTaskRepository propagateTaskRepository;
    private final PropagateTaskConvertor propagateTaskConvertor;

    @Autowired
    public PropagateTaskGatewayImpl(PropagateTaskRepository propagateTaskRepository) {
        this(propagateTaskRepository, Mappers.getMapper(PropagateTaskConvertor.class));
    }

    public PropagateTaskGatewayImpl(PropagateTaskRepository propagateTaskRepository, PropagateTaskConvertor propagateTaskConvertor) {
        this.propagateTaskRepository = propagateTaskRepository;
        this.propagateTaskConvertor = propagateTaskConvertor != null ? propagateTaskConvertor : Mappers.getMapper(PropagateTaskConvertor.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PropagateTask> findByClassificationAndVersion(String classificationId, String version) {
        PropagateTaskPO po = propagateTaskRepository.selectOne(
                Wrappers.<PropagateTaskPO>lambdaQuery()
                        .eq(PropagateTaskPO::getClassificationId, classificationId)
                        .eq(PropagateTaskPO::getVersion, version)
                        .last("LIMIT 1"));
        return po == null ? Optional.empty() : Optional.of(propagateTaskConvertor.toDomain(po));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PropagateTask save(PropagateTask task) {
        PropagateTaskPO po = propagateTaskConvertor.toPO(task);
        if (propagateTaskRepository.selectById(po.getId()) != null) {
            propagateTaskRepository.updateById(po);
        } else {
            propagateTaskRepository.insert(po);
        }
        return propagateTaskConvertor.toDomain(po);
    }
}
