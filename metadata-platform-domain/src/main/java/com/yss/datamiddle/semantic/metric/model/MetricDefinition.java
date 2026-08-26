package com.yss.datamiddle.semantic.metric.model;

import com.yss.datamiddle.semantic.term.exception.StateConflictException;
import com.yss.datamiddle.semantic.term.exception.VersionConflictException;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 指标口径聚合根（MetricDefinitionAggregate：metric_definition + metric_version 快照）。
 */
@Getter
@Setter
public class MetricDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String metricGroup;
    private String description;
    private String owner;
    private MetricStatus status;
    private Boolean authoritative;
    private String certifiedBy;
    private LocalDateTime certifiedAt;
    private Integer currentVersionNo;
    private List<MetricVersion> versions = new ArrayList<>();
    private Integer version;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MetricDefinition create(String name, String metricGroup, String description, String owner, String operator) {
        MetricDefinition m = new MetricDefinition();
        m.name = name;
        m.metricGroup = metricGroup;
        m.description = description;
        m.owner = owner;
        m.status = MetricStatus.DRAFT;
        m.authoritative = false;
        m.currentVersionNo = 0;
        m.version = 0;
        m.createdBy = operator;
        m.createdAt = LocalDateTime.now();
        m.updatedAt = m.createdAt;
        return m;
    }

    public MetricVersion addVersion(String expression, String logicDesc, List<String> dimensions, String filters, String operator) {
        int nextVersionNo = (this.currentVersionNo == null ? 0 : this.currentVersionNo) + 1;
        MetricVersion mv = MetricVersion.builder()
                .metricId(this.id)
                .versionNo(nextVersionNo)
                .expression(expression)
                .logicDescription(logicDesc)
                .dimensions(dimensions == null ? new ArrayList<>() : new ArrayList<>(dimensions))
                .filters(filters)
                .createdBy(operator)
                .createdAt(LocalDateTime.now())
                .build();

        this.versions.add(mv);
        this.currentVersionNo = nextVersionNo;
        if (this.status == MetricStatus.DRAFT) {
            this.status = MetricStatus.ACTIVE;
        }
        this.updatedAt = LocalDateTime.now();
        return mv;
    }

    public MetricVersion rollbackTo(Integer versionNo, String operator) {
        MetricVersion targetSnapshot = this.versions.stream()
                .filter(v -> Objects.equals(v.getVersionNo(), versionNo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到历史快照版本: " + versionNo));

        int nextVersionNo = this.currentVersionNo + 1;
        MetricVersion rollbackVersion = MetricVersion.builder()
                .metricId(this.id)
                .versionNo(nextVersionNo)
                .expression(targetSnapshot.getExpression())
                .logicDescription(targetSnapshot.getLogicDescription())
                .dimensions(new ArrayList<>(targetSnapshot.getDimensions()))
                .filters(targetSnapshot.getFilters())
                .rollbackFromNo(versionNo)
                .createdBy(operator)
                .createdAt(LocalDateTime.now())
                .build();

        this.versions.add(rollbackVersion);
        this.currentVersionNo = nextVersionNo;
        this.updatedAt = LocalDateTime.now();
        return rollbackVersion;
    }

    public MetricDefinition certify(boolean force, MetricDefinition existingCertifiedInGroup, String operator) {
        if (this.status == MetricStatus.DEPRECATED) {
            throw new StateConflictException("已弃用口径不可再认证");
        }
        if (Boolean.TRUE.equals(this.authoritative)) {
            return null; // 幂等
        }

        if (existingCertifiedInGroup != null && !Objects.equals(existingCertifiedInGroup.getId(), this.id)) {
            if (!force) {
                throw new StateConflictException("AUTH_CONFLICT: 该指标组已存在认证口径 [" + existingCertifiedInGroup.getName() + "]");
            }
            // 自动使旧认证失效 (SB-02)
            existingCertifiedInGroup.invalidateCertification();
        }

        this.authoritative = true;
        this.certifiedBy = operator;
        this.certifiedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        return existingCertifiedInGroup;
    }

    public void invalidateCertification() {
        this.authoritative = false;
        this.certifiedBy = null;
        this.certifiedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void deprecate(String operator) {
        this.status = MetricStatus.DEPRECATED;
        this.authoritative = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void toggleStatus(MetricStatus targetStatus, String operator) {
        if (this.status == MetricStatus.DEPRECATED) {
            throw new StateConflictException("已弃用口径不可再变更状态");
        }
        this.status = targetStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateInfo(String name, String metricGroup, String desc, String owner, Integer expectedVersion, String operator) {
        if (!Objects.equals(this.version, expectedVersion)) {
            throw new com.yss.datamiddle.semantic.metric.exception.MetricVersionConflictException("版本冲突，当前版本为 " + this.version, this);
        }
        this.name = name;
        this.metricGroup = metricGroup;
        this.description = desc;
        this.owner = owner;
        // SB-02: 变更后认证失效退回未认证
        if (Boolean.TRUE.equals(this.authoritative)) {
            invalidateCertification();
        }
        this.version = (this.version == null ? 0 : this.version) + 1;
        this.updatedAt = LocalDateTime.now();
    }
}
