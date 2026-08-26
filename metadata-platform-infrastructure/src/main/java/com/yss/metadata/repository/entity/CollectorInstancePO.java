package com.yss.metadata.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.yss.metadata.domain.collector.model.MetadataDiffSummary;
import com.yss.metadata.domain.collector.model.WorkflowNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采集实例持久化对象。
 *
 * <p>对应 collector_instance 表；复杂嵌套结构 workflow_nodes 与 diff_summary 采用 JacksonTypeHandler 处理 JSON 序列化。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "collector_instance", autoResultMap = true)
public class CollectorInstancePO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("name")
    private String name;

    @TableField("collector_id")
    private String collectorId;

    @TableField("collector_name")
    private String collectorName;

    @TableField("connector_id")
    private String connectorId;

    @TableField("connector_name")
    private String connectorName;

    @TableField("datasource_type")
    private String datasourceType;

    @TableField("status")
    private String status;

    @TableField("execution_mode")
    private String executionMode;

    @TableField("schedule_description")
    private String scheduleDescription;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("duration_ms")
    private Long durationMs;

    @TableField("executor")
    private String executor;

    @TableField("owner")
    private String owner;

    @TableField("error_message")
    private String errorMessage;

    @TableField("is_dry_run")
    private Boolean isDryRun;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("max_retries")
    private Integer maxRetries;

    @TableField(value = "workflow_nodes", typeHandler = com.yss.metadata.repository.typehandler.WorkflowNodesTypeHandler.class)
    private List<WorkflowNode> workflowNodes;

    @TableField(value = "diff_summary", typeHandler = com.yss.metadata.repository.typehandler.DiffSummaryTypeHandler.class)
    private MetadataDiffSummary diffSummary;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
