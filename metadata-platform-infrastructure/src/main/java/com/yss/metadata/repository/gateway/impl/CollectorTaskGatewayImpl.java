package com.yss.metadata.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.metadata.domain.collector.gateway.CollectorTaskGateway;
import com.yss.metadata.domain.collector.model.CollectSchedule;
import com.yss.metadata.domain.collector.model.CollectorTask;
import com.yss.metadata.repository.CollectorTaskRepository;
import com.yss.metadata.infrastructure.convertor.CollectorTaskConvertor;
import com.yss.metadata.repository.entity.CollectorTaskPO;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 采集任务仓储网关实现（MyBatis-Plus，BasePlusRepository + LambdaQueryWrapper）。
 */
@Repository
public class CollectorTaskGatewayImpl implements CollectorTaskGateway {

    private final CollectorTaskRepository collectorTaskRepository;
    private final CollectorTaskConvertor collectorTaskConvertor;

    @Autowired
    public CollectorTaskGatewayImpl(CollectorTaskRepository collectorTaskRepository) {
        this(collectorTaskRepository, Mappers.getMapper(CollectorTaskConvertor.class));
    }

    public CollectorTaskGatewayImpl(CollectorTaskRepository collectorTaskRepository, CollectorTaskConvertor collectorTaskConvertor) {
        this.collectorTaskRepository = collectorTaskRepository;
        this.collectorTaskConvertor = collectorTaskConvertor != null ? collectorTaskConvertor : Mappers.getMapper(CollectorTaskConvertor.class);
    }

    @Override
    public List<CollectorTask> findAll() {
        return collectorTaskConvertor.toCollectorTaskList(collectorTaskRepository.selectList(null));
    }

    @Override
    public List<CollectorTask> findByQuery(com.yss.metadata.client.dto.query.CollectorQuery query) {
        if (query == null) {
            return findAll();
        }
        LambdaQueryWrapper<CollectorTaskPO> wrapper = Wrappers.<CollectorTaskPO>lambdaQuery();
        if (query.getKeyword() != null && !query.getKeyword().trim().isEmpty()) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(CollectorTaskPO::getName, kw).or().like(CollectorTaskPO::getConnectorId, kw));
        }
        if (query.getOwner() != null && !query.getOwner().trim().isEmpty()) {
            wrapper.eq(CollectorTaskPO::getOwner, query.getOwner().trim());
        }
        if (query.getEnabled() != null) {
            wrapper.eq(CollectorTaskPO::getEnabled, query.getEnabled());
        }
        if (query.getDatasourceType() != null && !query.getDatasourceType().trim().isEmpty()) {
            wrapper.eq(CollectorTaskPO::getDatasourceType, query.getDatasourceType().trim());
        }
        if (query.getMode() != null && !query.getMode().trim().isEmpty()) {
            wrapper.eq(CollectorTaskPO::getMode, query.getMode().trim());
        }
        if (query.getStatus() != null && !query.getStatus().trim().isEmpty()) {
            wrapper.eq(CollectorTaskPO::getStatus, query.getStatus().trim());
        }
        wrapper.orderByDesc(CollectorTaskPO::getUpdatedAt);
        return collectorTaskConvertor.toCollectorTaskList(collectorTaskRepository.selectList(wrapper));
    }

    @Override
    public Optional<CollectorTask> findById(String id) {
        return Optional.ofNullable(collectorTaskRepository.selectById(id))
                .map(collectorTaskConvertor::toCollectorTask);
    }

    @Override
    public boolean existsByConnectorIdAndSchedule(String connectorId, CollectSchedule schedule) {
        return collectorTaskRepository.selectCount(sourceScheduleWrapper(connectorId, schedule)) > 0;
    }

    @Override
    public boolean existsByConnectorIdAndScheduleExcluding(String connectorId, CollectSchedule schedule,
                                                           String excludeId) {
        return collectorTaskRepository.selectCount(
                sourceScheduleWrapper(connectorId, schedule).ne(CollectorTaskPO::getId, excludeId)) > 0;
    }

    @Override
    public boolean existsByConnectorId(String connectorId) {
        return collectorTaskRepository.selectCount(
                Wrappers.<CollectorTaskPO>lambdaQuery().eq(CollectorTaskPO::getConnectorId, connectorId)) > 0;
    }

    @Override
    public CollectorTask save(CollectorTask task) {
        CollectorTaskPO po = collectorTaskConvertor.toPO(task);
        if (collectorTaskRepository.selectById(po.getId()) != null) {
            collectorTaskRepository.updateById(po);
        } else {
            collectorTaskRepository.insert(po);
        }
        return task;
    }

    @Override
    public void deleteById(String id) {
        collectorTaskRepository.deleteById(id);
    }

    private LambdaQueryWrapper<CollectorTaskPO> sourceScheduleWrapper(String connectorId, CollectSchedule schedule) {
        return Wrappers.<CollectorTaskPO>lambdaQuery()
                .eq(CollectorTaskPO::getConnectorId, connectorId)
                .eq(CollectorTaskPO::getSchedule, schedule.getValue());
    }
}
