package com.yss.datamiddle.semantic.infrastructure.repository.gateway.impl;

import com.yss.datamiddle.semantic.metric.gateway.MetricGateway;
import com.yss.datamiddle.semantic.metric.model.MetricDefinition;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 指标口径持久化网关实现（SL-002 / SL-006）。
 */
@Repository
public class MetricGatewayImpl implements MetricGateway {

    private final Map<Long, MetricDefinition> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1000);

    @Override
    public MetricDefinition save(MetricDefinition metric) {
        if (metric.getId() == null) {
            metric.setId(idGen.incrementAndGet());
        }
        storage.put(metric.getId(), metric);
        return metric;
    }

    @Override
    public MetricDefinition update(MetricDefinition metric) {
        storage.put(metric.getId(), metric);
        return metric;
    }

    @Override
    public Optional<MetricDefinition> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<MetricDefinition> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return storage.values().stream()
                .filter(m -> name.equalsIgnoreCase(m.getName()))
                .findFirst();
    }

    @Override
    public Optional<MetricDefinition> findAuthoritativeInGroup(String metricGroup) {
        if (metricGroup == null) {
            return Optional.empty();
        }
        return storage.values().stream()
                .filter(m -> metricGroup.equalsIgnoreCase(m.getMetricGroup()) && Boolean.TRUE.equals(m.getAuthoritative()))
                .findFirst();
    }

    @Override
    public List<MetricDefinition> findByGroup(String metricGroup) {
        if (metricGroup == null) {
            return new ArrayList<>();
        }
        return storage.values().stream()
                .filter(m -> metricGroup.equalsIgnoreCase(m.getMetricGroup()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MetricDefinition> listAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public boolean delete(Long id) {
        return storage.remove(id) != null;
    }
}
