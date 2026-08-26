package com.yss.metadata.client.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 采集实例视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "采集实例视图对象")
public class CollectorInstanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "实例 ID")
    private String id;

    @ApiModelProperty(value = "实例名称")
    private String name;

    @ApiModelProperty(value = "关联采集任务 ID")
    private String collectorId;

    @ApiModelProperty(value = "关联采集任务名称")
    private String collectorName;

    @ApiModelProperty(value = "关联数据源 ID")
    private String connectorId;

    @ApiModelProperty(value = "关联数据源名称")
    private String connectorName;

    @ApiModelProperty(value = "数据源类型 (MySQL, Oracle, ClickHouse 等)")
    private String datasourceType;

    @ApiModelProperty(value = "执行状态 (pending, running, success, failed)")
    private String status;

    @ApiModelProperty(value = "执行状态描述")
    private String statusDescription;

    @ApiModelProperty(value = "执行方式 (manual, schedule, auto_retry, dry_run)")
    private String executionMode;

    @ApiModelProperty(value = "执行方式描述")
    private String executionModeDescription;

    @ApiModelProperty(value = "调度周期描述 (如: 每日, 04:17)")
    private String scheduleDescription;

    @ApiModelProperty(value = "开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "耗时 (毫秒)")
    private Long durationMs;

    @ApiModelProperty(value = "执行人")
    private String executor;

    @ApiModelProperty(value = "任务负责人")
    private String owner;

    @ApiModelProperty(value = "错误信息 / 终止说明")
    private String errorMessage;

    @ApiModelProperty(value = "是否空跑实例")
    private Boolean isDryRun;

    @ApiModelProperty(value = "当前重试次数")
    private Integer retryCount;

    @ApiModelProperty(value = "最大重试次数")
    private Integer maxRetries;
}
