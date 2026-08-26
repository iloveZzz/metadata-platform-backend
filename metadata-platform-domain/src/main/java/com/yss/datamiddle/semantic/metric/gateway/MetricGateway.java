package com.yss.datamiddle.semantic.metric.gateway;

import com.yss.datamiddle.semantic.metric.model.MetricDefinition;

import java.util.List;
import java.util.Optional;

public interface MetricGateway {
    MetricDefinition save(MetricDefinition metric);
    MetricDefinition update(MetricDefinition metric);
    Optional<MetricDefinition> findById(Long id);
    Optional<MetricDefinition> findByName(String name);
    Optional<MetricDefinition> findAuthoritativeInGroup(String metricGroup);
    List<MetricDefinition> findByGroup(String metricGroup);
    List<MetricDefinition> listAll();
    boolean delete(Long id);
}
