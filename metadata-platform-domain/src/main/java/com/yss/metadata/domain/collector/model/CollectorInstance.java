package com.yss.metadata.domain.collector.model;

import com.yss.metadata.domain.collector.exception.CollectorInstanceStateConflictException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 采集任务实例聚合根。
 *
 * <p>状态机：
 * <ul>
 *   <li>等待中 (pending) -> 运行中 (running) -> 成功 (success) / 失败 (failed)</li>
 *   <li>终止操作：仅运行中 (running)、等待中 (pending) 支持，终止后状态置为失败 (failed)</li>
 *   <li>重跑操作：仅失败 (failed) 支持，重跑后状态置为等待中 (pending) 并重新调度执行</li>
 * </ul>
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectorInstance implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 实例 ID */
    private String id;

    /** 实例名称 */
    private String name;

    /** 关联采集任务 ID */
    private String collectorId;

    /** 关联采集任务名称 */
    private String collectorName;

    /** 关联数据源 ID */
    private String connectorId;

    /** 关联数据源名称 */
    private String connectorName;

    /** 数据源类型 (MySQL, Oracle, ClickHouse 等) */
    private String datasourceType;

    /** 执行状态 */
    private CollectorInstanceStatus status;

    /** 执行方式 */
    private ExecutionMode executionMode;

    /** 调度周期 / 触发描述 (如: 每日, 04:17) */
    private String scheduleDescription;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 执行耗时 (毫秒) */
    private Long durationMs;

    /** 执行人 (工号/名称，或 System) */
    private String executor;

    /** 任务负责人 (工号/名称) */
    private String owner;

    /** 错误信息 / 终止原因 */
    private String errorMessage;

    /** 是否空跑实例 (月度任务默认生成空跑实例并置为成功) */
    @Builder.Default
    private Boolean isDryRun = Boolean.FALSE;

    /** 当前重试次数 */
    @Builder.Default
    private Integer retryCount = 0;

    /** 最大重试次数 */
    @Builder.Default
    private Integer maxRetries = 3;

    /** 工作流节点列表 */
    @Builder.Default
    private List<WorkflowNode> workflowNodes = new ArrayList<>();

    /** 变更概览摘要 */
    private MetadataDiffSummary diffSummary;

    /**
     * 终止实例执行（仅运行中、等待中支持）
     */
    public void terminate(String operator, String reason) {
        if (this.status != CollectorInstanceStatus.RUNNING && this.status != CollectorInstanceStatus.PENDING) {
            throw new CollectorInstanceStateConflictException(
                    "仅运行中和等待中状态的实例支持终止操作，当前状态为: " + (this.status != null ? this.status.getDescription() : "未知")
            );
        }
        this.status = CollectorInstanceStatus.FAILED;
        this.endTime = LocalDateTime.now();
        if (this.startTime != null) {
            this.durationMs = Duration.between(this.startTime, this.endTime).toMillis();
        }
        String termReason = (reason != null && !reason.trim().isEmpty()) ? reason : "用户手动终止";
        this.errorMessage = (operator != null ? "[" + operator + "] " : "") + termReason;

        // 终止所有未完成的工作流节点
        if (this.workflowNodes != null) {
            for (WorkflowNode node : this.workflowNodes) {
                if (node.getStatus() == CollectorInstanceStatus.RUNNING || node.getStatus() == CollectorInstanceStatus.PENDING) {
                    node.setStatus(CollectorInstanceStatus.FAILED);
                    node.setEndTime(this.endTime);
                    node.setExceptionInfo(this.errorMessage);
                }
            }
        }
    }

    /**
     * 重跑实例（仅失败状态支持）
     */
    public void rerun(String operator) {
        if (this.status != CollectorInstanceStatus.FAILED) {
            throw new CollectorInstanceStateConflictException(
                    "仅失败状态的实例支持重跑操作，当前状态为: " + (this.status != null ? this.status.getDescription() : "未知")
            );
        }
        this.status = CollectorInstanceStatus.RUNNING;
        this.startTime = LocalDateTime.now();
        this.endTime = null;
        this.durationMs = null;
        this.errorMessage = null;
        if (operator != null && !operator.trim().isEmpty()) {
            this.executor = operator;
        }

        // 重置工作流节点
        if (this.workflowNodes != null) {
            for (WorkflowNode node : this.workflowNodes) {
                node.setStatus(CollectorInstanceStatus.RUNNING);
                node.setStartTime(this.startTime);
                node.setEndTime(null);
                node.setExceptionInfo(null);
                node.rerun();
            }
        }
    }

    /**
     * 标记为空跑成功
     */
    public void markDryRunSuccess() {
        this.isDryRun = Boolean.TRUE;
        this.executionMode = ExecutionMode.DRY_RUN;
        this.status = CollectorInstanceStatus.SUCCESS;
        this.startTime = LocalDateTime.now();
        this.endTime = this.startTime.plusSeconds(2);
        this.durationMs = 2000L;
    }
}
