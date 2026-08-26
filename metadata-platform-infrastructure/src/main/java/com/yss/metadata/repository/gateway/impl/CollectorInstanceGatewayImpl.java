package com.yss.metadata.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.metadata.client.dto.query.CollectorInstanceQuery;
import com.yss.metadata.domain.collector.gateway.CollectorInstanceGateway;
import com.yss.metadata.domain.collector.model.CollectorInstance;
import com.yss.metadata.domain.collector.model.CollectorInstanceStatus;
import com.yss.metadata.infrastructure.convertor.CollectorInstanceConvertor;
import com.yss.metadata.repository.CollectorInstanceRepository;
import com.yss.metadata.repository.entity.CollectorInstancePO;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 采集实例仓储网关实现（基于 MyBatis-Plus 数据库物理表 collector_instance 持久化）。
 */
@Repository
public class CollectorInstanceGatewayImpl implements CollectorInstanceGateway {

    private final CollectorInstanceRepository collectorInstanceRepository;
    private final CollectorInstanceConvertor collectorInstanceConvertor;

    @Autowired
    public CollectorInstanceGatewayImpl(CollectorInstanceRepository collectorInstanceRepository) {
        this(collectorInstanceRepository, Mappers.getMapper(CollectorInstanceConvertor.class));
    }

    public CollectorInstanceGatewayImpl(CollectorInstanceRepository collectorInstanceRepository,
                                       CollectorInstanceConvertor collectorInstanceConvertor) {
        this.collectorInstanceRepository = collectorInstanceRepository;
        this.collectorInstanceConvertor = collectorInstanceConvertor != null ? collectorInstanceConvertor : Mappers.getMapper(CollectorInstanceConvertor.class);
    }

    @Override
    public List<CollectorInstance> findAll() {
        LambdaQueryWrapper<CollectorInstancePO> wrapper = Wrappers.<CollectorInstancePO>lambdaQuery()
                .orderByDesc(CollectorInstancePO::getStartTime);
        return collectorInstanceConvertor.toCollectorInstanceList(collectorInstanceRepository.selectList(wrapper));
    }

    @Override
    public List<CollectorInstance> findByQuery(CollectorInstanceQuery query) {
        if (query == null) {
            return findAll();
        }
        LambdaQueryWrapper<CollectorInstancePO> wrapper = Wrappers.<CollectorInstancePO>lambdaQuery();

        // 关键字搜索 (匹配实例名称、采集任务名称、数据源名称)
        if (query.getKeyword() != null && !query.getKeyword().trim().isEmpty()) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(CollectorInstancePO::getName, kw)
                    .or().like(CollectorInstancePO::getCollectorName, kw)
                    .or().like(CollectorInstancePO::getConnectorName, kw));
        }
        // 负责人过滤
        if (query.getOwner() != null && !query.getOwner().trim().isEmpty()) {
            wrapper.eq(CollectorInstancePO::getOwner, query.getOwner().trim());
        }
        // 执行人过滤
        if (query.getExecutor() != null && !query.getExecutor().trim().isEmpty()) {
            wrapper.eq(CollectorInstancePO::getExecutor, query.getExecutor().trim());
        }
        // 仅失败实例或指定状态
        if (Boolean.TRUE.equals(query.getOnlyFailed())) {
            wrapper.eq(CollectorInstancePO::getStatus, CollectorInstanceStatus.FAILED.getCode());
        } else if (query.getStatus() != null && !query.getStatus().trim().isEmpty()) {
            wrapper.eq(CollectorInstancePO::getStatus, query.getStatus().trim());
        }
        // 数据源类型
        if (query.getDatasourceType() != null && !query.getDatasourceType().trim().isEmpty()) {
            wrapper.eq(CollectorInstancePO::getDatasourceType, query.getDatasourceType().trim());
        }
        // 执行方式
        if (query.getExecutionMode() != null && !query.getExecutionMode().trim().isEmpty()) {
            wrapper.eq(CollectorInstancePO::getExecutionMode, query.getExecutionMode().trim());
        }
        // 关联采集任务 ID
        if (query.getCollectorId() != null && !query.getCollectorId().trim().isEmpty()) {
            wrapper.eq(CollectorInstancePO::getCollectorId, query.getCollectorId().trim());
        }
        // 开始时间范围
        LocalDateTime begin = parseDateTime(query.getStartTimeBegin());
        if (begin != null) {
            wrapper.ge(CollectorInstancePO::getStartTime, begin);
        }
        LocalDateTime end = parseDateTime(query.getStartTimeEnd());
        if (end != null) {
            wrapper.le(CollectorInstancePO::getStartTime, end);
        }

        wrapper.orderByDesc(CollectorInstancePO::getStartTime);
        return collectorInstanceConvertor.toCollectorInstanceList(collectorInstanceRepository.selectList(wrapper));
    }

    @Override
    public Optional<CollectorInstance> findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(collectorInstanceRepository.selectById(id))
                .map(collectorInstanceConvertor::toCollectorInstance);
    }

    @Override
    public List<CollectorInstance> findByCollectorId(String collectorId) {
        if (collectorId == null || collectorId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<CollectorInstancePO> wrapper = Wrappers.<CollectorInstancePO>lambdaQuery()
                .eq(CollectorInstancePO::getCollectorId, collectorId.trim())
                .orderByDesc(CollectorInstancePO::getStartTime);
        return collectorInstanceConvertor.toCollectorInstanceList(collectorInstanceRepository.selectList(wrapper));
    }

    @Override
    public CollectorInstance save(CollectorInstance instance) {
        if (instance == null) {
            return null;
        }
        if (instance.getId() == null || instance.getId().trim().isEmpty()) {
            instance.setId("inst-" + UUID.randomUUID().toString().replace("-", ""));
        }
        CollectorInstancePO po = collectorInstanceConvertor.toPO(instance);
        if (collectorInstanceRepository.selectById(po.getId()) != null) {
            collectorInstanceRepository.updateById(po);
        } else {
            collectorInstanceRepository.insert(po);
        }
        return instance;
    }

    @Override
    public List<CollectorInstance> saveAll(List<CollectorInstance> instances) {
        if (instances == null) {
            return Collections.emptyList();
        }
        for (CollectorInstance instance : instances) {
            save(instance);
        }
        return instances;
    }

    @Override
    public void deleteById(String id) {
        if (id != null && !id.trim().isEmpty()) {
            collectorInstanceRepository.deleteById(id);
        }
    }

    private LocalDateTime parseDateTime(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String s = text.trim();
        try {
            if (s.length() == 10) {
                return LocalDate.parse(s).atStartOfDay();
            }
            if (s.contains(" ") && !s.contains("T")) {
                return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            return LocalDateTime.parse(s);
        } catch (Exception ignored) {
            return null;
        }
    }
}
