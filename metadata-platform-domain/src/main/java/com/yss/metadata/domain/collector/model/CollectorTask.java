package com.yss.metadata.domain.collector.model;

import com.yss.metadata.domain.collector.exception.CollectorTaskStateConflictException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 采集任务聚合根（系统概要设计 §5 / §8，spec FR-005）。
 *
 * <p>状态机：待执行 → 运行中 → 成功 / 失败 / 已取消。
 * 核心规则（主控语义纠偏后）：
 * <ul>
 *   <li>运行中不可重复触发（幂等拒绝，409 语义）；待执行/成功/失败/已取消均允许开始执行（可重新执行）；</li>
 *   <li>取消仅运行中可执行（其余状态抛状态冲突，409 语义）；</li>
 *   <li>运行中可标记成功 / 失败；失败携带失败原因（局部重采语义字段，实际重采逻辑后置）；</li>
 *   <li>配置变更后重置为待执行，需重新触发执行。</li>
 * </ul></p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectorTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private String id;

    /** 任务名称 */
    private String name;

    /** 目标数据源（连接器 id） */
    private String connectorId;

    /** 调度（cron 或周期描述） */
    private CollectSchedule schedule;

    /** 采集模式（增量/全量） */
    private CollectorMode mode;

    /** 覆盖策略 */
    private CollectorStrategy strategy;

    /** 是否自动识别分类（默认 true） */
    private Boolean autoClassify;

    /** 当前状态 */
    private CollectorTaskStatus status;

    /** 失败原因（局部重采语义字段，实际重采逻辑 WU-01-03 后置） */
    private String failReason;

    /** 最近一次执行开始时间 */
    private LocalDateTime lastRunAt;

    /** 负责人（工号/用户ID） */
    private String owner;

    /** 任务描述 */
    private String description;

    /** 生效状态（默认 true） */
    private Boolean enabled;

    /** 数据源类型（如 MySQL, Oracle, ClickHouse 等） */
    private String datasourceType;

    /** 业务来源系统 */
    private String sourceSystem;

    /** 采集范围（all / custom） */
    private String scopeType;

    /** 指定 Database 列表（逗号分隔或 JSON） */
    private String selectedDatabases;

    /** 是否开启失败重试 */
    private Boolean retryEnabled;

    /** 重试次数 */
    private Integer retryCount;

    /** 重试间隔（分钟） */
    private Integer retryInterval;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 最后更新时间 */
    private LocalDateTime updatedAt;

    /**
     * 开始执行 → 运行中。
     *
     * <p>待执行/成功/失败/已取消均可开始执行（支持重新执行与失败重跑）；
     * 仅运行中再次触发抛状态冲突（幂等拒绝，409 语义）；新一轮执行清空旧失败原因。</p>
     */
    public void start() {
        if (status == CollectorTaskStatus.RUNNING) {
            throw new CollectorTaskStateConflictException("采集任务运行中，不可重复触发");
        }
        this.status = CollectorTaskStatus.RUNNING;
        this.lastRunAt = LocalDateTime.now();
        this.failReason = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 取消：仅运行中 → 已取消（其余状态抛状态冲突，409 语义）。
     */
    public void cancel() {
        if (status != CollectorTaskStatus.RUNNING) {
            throw new CollectorTaskStateConflictException(
                    "仅运行中的采集任务可取消，当前状态为 " + status.getDescription());
        }
        this.status = CollectorTaskStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记成功：仅运行中 → 成功，并清空失败原因。
     */
    public void markSucceeded() {
        requireRunning("标记成功");
        this.status = CollectorTaskStatus.SUCCESS;
        this.failReason = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记失败：仅运行中 → 失败，携带失败原因（局部重采语义字段）。
     */
    public void markFailed(String cause) {
        requireRunning("标记失败");
        this.status = CollectorTaskStatus.FAILED;
        this.failReason = cause;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 切换生效状态。
     */
     public void toggleEnabled(Boolean enabled) {
         this.enabled = enabled != null && enabled;
         this.updatedAt = LocalDateTime.now();
     }

    /**
     * 配置更新（PUT 全量替换语义）。
     *
     * <p>配置变更后状态重置为待执行，需重新触发执行；旧失败原因清空；更新时间刷新。</p>
     */
    public void update(String name, String connectorId, CollectSchedule schedule, CollectorMode mode,
                       CollectorStrategy strategy, Boolean autoClassify) {
        this.name = name;
        this.connectorId = connectorId;
        this.schedule = schedule;
        this.mode = mode;
        this.strategy = strategy;
        this.autoClassify = autoClassify == null || autoClassify;
        this.status = CollectorTaskStatus.PENDING;
        this.failReason = null;
        this.updatedAt = LocalDateTime.now();
        validate();
    }

    /**
     * 扩展详情配置更新。
     */
    public void updateDetails(String name, String connectorId, CollectSchedule schedule, CollectorMode mode,
                              CollectorStrategy strategy, Boolean autoClassify, String owner, String description,
                              Boolean enabled, String datasourceType, String sourceSystem, String scopeType,
                              String selectedDatabases, Boolean retryEnabled, Integer retryCount, Integer retryInterval) {
        this.name = name;
        this.connectorId = connectorId;
        this.schedule = schedule;
        this.mode = mode;
        this.strategy = strategy;
        this.autoClassify = autoClassify == null || autoClassify;
        if (owner != null && !owner.trim().isEmpty()) {
            this.owner = owner.trim();
        }
        this.description = description;
        this.enabled = enabled == null || enabled;
        this.datasourceType = datasourceType;
        this.sourceSystem = sourceSystem;
        this.scopeType = scopeType;
        this.selectedDatabases = selectedDatabases;
        this.retryEnabled = retryEnabled != null && retryEnabled;
        this.retryCount = retryCount;
        this.retryInterval = retryInterval;
        this.status = CollectorTaskStatus.PENDING;
        this.failReason = null;
        this.updatedAt = LocalDateTime.now();
        validate();
    }

    /**
     * 领域不变量校验：名称/目标数据源/调度/采集模式/覆盖策略非空。
     */
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("采集任务名称不能为空");
        }
        if (connectorId == null || connectorId.trim().isEmpty()) {
            throw new IllegalArgumentException("目标数据源不能为空");
        }
        if (schedule == null) {
            throw new IllegalArgumentException("调度不能为空");
        }
        if (mode == null) {
            throw new IllegalArgumentException("采集模式不能为空");
        }
        if (strategy == null) {
            throw new IllegalArgumentException("覆盖策略不能为空");
        }
    }

    private void requireRunning(String action) {
        if (status != CollectorTaskStatus.RUNNING) {
            throw new CollectorTaskStateConflictException(
                    action + "仅允许运行中的采集任务执行，当前状态为 " + status.getDescription());
        }
    }
}
