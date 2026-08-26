package com.yss.datamiddle.semantic.application.service;

import com.yss.datamiddle.semantic.application.model.MetricCreateInput;
import com.yss.datamiddle.semantic.application.model.MetricVersionInput;
import com.yss.datamiddle.semantic.application.port.CurrentUserPort;
import com.yss.datamiddle.semantic.metric.exception.MetricNameDuplicateException;
import com.yss.datamiddle.semantic.metric.exception.MetricNotFoundException;
import com.yss.datamiddle.semantic.metric.gateway.MetricGateway;
import com.yss.datamiddle.semantic.metric.model.MetricDefinition;
import com.yss.datamiddle.semantic.metric.model.MetricStatus;
import com.yss.datamiddle.semantic.metric.model.MetricVersion;
import com.yss.datamiddle.semantic.term.exception.PermissionDeniedException;
import com.yss.datamiddle.semantic.term.exception.StateConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 指标口径应用服务（SL-002 / SL-006）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetricService {

    private final MetricGateway metricGateway;
    private final CurrentUserPort currentUserPort;

    public MetricDefinition create(MetricCreateInput input) {
        checkWritePermission();
        String operator = currentUserPort.userName();
        if (metricGateway.findByName(input.getName()).isPresent()) {
            throw new MetricNameDuplicateException(input.getName());
        }
        MetricDefinition m = MetricDefinition.create(
                input.getName(),
                input.getMetricGroup(),
                input.getDescription(),
                input.getOwner(),
                operator
        );
        return metricGateway.save(m);
    }

    public MetricVersion addVersion(Long id, MetricVersionInput input) {
        checkWritePermission();
        String operator = currentUserPort.userName();
        MetricDefinition m = metricGateway.findById(id)
                .orElseThrow(() -> new MetricNotFoundException(id));

        MetricVersion version = m.addVersion(
                input.getExpression(),
                input.getLogicDescription(),
                input.getDimensions(),
                input.getFilters(),
                operator
        );
        metricGateway.update(m);
        return version;
    }

    public MetricVersion rollback(Long id, Integer versionNo) {
        checkWritePermission();
        String operator = currentUserPort.userName();
        MetricDefinition m = metricGateway.findById(id)
                .orElseThrow(() -> new MetricNotFoundException(id));

        MetricVersion rollbackVersion = m.rollbackTo(versionNo, operator);
        metricGateway.update(m);
        return rollbackVersion;
    }

    public MetricDefinition certify(Long id, boolean force) {
        checkWritePermission();
        String operator = currentUserPort.userName();
        MetricDefinition m = metricGateway.findById(id)
                .orElseThrow(() -> new MetricNotFoundException(id));

        MetricDefinition existingInGroup = metricGateway.findAuthoritativeInGroup(m.getMetricGroup())
                .orElse(null);

        MetricDefinition superseded = m.certify(force, existingInGroup, operator);
        if (superseded != null) {
            metricGateway.update(superseded);
        }
        return metricGateway.update(m);
    }

    public void deprecate(Long id) {
        checkWritePermission();
        String operator = currentUserPort.userName();
        MetricDefinition m = metricGateway.findById(id)
                .orElseThrow(() -> new MetricNotFoundException(id));
        m.deprecate(operator);
        metricGateway.update(m);
    }

    public void toggleStatus(Long id, MetricStatus targetStatus) {
        checkWritePermission();
        String operator = currentUserPort.userName();
        MetricDefinition m = metricGateway.findById(id)
                .orElseThrow(() -> new MetricNotFoundException(id));
        m.toggleStatus(targetStatus, operator);
        metricGateway.update(m);
    }

    public void delete(Long id) {
        checkWritePermission();
        MetricDefinition m = metricGateway.findById(id)
                .orElseThrow(() -> new MetricNotFoundException(id));
        if (m.getCurrentVersionNo() != null && m.getCurrentVersionNo() > 0) {
            throw new StateConflictException("已有版本的指标口径不可直接删除，请使用停用或弃用");
        }
        metricGateway.delete(id);
    }

    public List<MetricDefinition> list() {
        return metricGateway.listAll();
    }

    public MetricDefinition getById(Long id) {
        return metricGateway.findById(id)
                .orElseThrow(() -> new MetricNotFoundException(id));
    }

    private void checkWritePermission() {
        if (!currentUserPort.isWritePermitted()) {
            throw new PermissionDeniedException("只读用户禁止执行写操作");
        }
    }
}
