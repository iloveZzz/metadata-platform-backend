package com.yss.metadata.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 采集任务持久化对象。
 *
 * <p>对应 collector_task 表（数据架构 §5）；id 采用 VARCHAR(36) UUID，
 * 由应用层生成，@TableId 使用 IdType.INPUT 手动赋值。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("collector_task")
public class CollectorTaskPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("name")
    private String name;

    @TableField("connector_id")
    private String connectorId;

    @TableField("schedule")
    private String schedule;

    @TableField("mode")
    private String mode;

    @TableField("strategy")
    private String strategy;

    @TableField("auto_classify")
    private Boolean autoClassify;

    @TableField("status")
    private String status;

    @TableField("fail_reason")
    private String failReason;

    @TableField("last_run_at")
    private LocalDateTime lastRunAt;

    @TableField("owner")
    private String owner;

    @TableField("description")
    private String description;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("datasource_type")
    private String datasourceType;

    @TableField("source_system")
    private String sourceSystem;

    @TableField("scope_type")
    private String scopeType;

    @TableField("selected_databases")
    private String selectedDatabases;

    @TableField("retry_enabled")
    private Boolean retryEnabled;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("retry_interval")
    private Integer retryInterval;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
