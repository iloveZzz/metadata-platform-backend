package com.yss.metadata.client.dto.query;

import com.yss.cloud.dto.QueryDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 采集实例查询请求参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "采集实例查询参数")
public class CollectorInstanceQuery extends QueryDTO {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "关键字 (实例名称/数据源名称模糊搜索)")
    private String keyword;

    @ApiModelProperty(value = "我负责的任务实例 (当前用户工号)")
    private String owner;

    @ApiModelProperty(value = "我执行的 (当前用户工号)")
    private String executor;

    @ApiModelProperty(value = "仅失败实例 (true/false)")
    private Boolean onlyFailed;

    @ApiModelProperty(value = "数据源类型 (MySQL, Oracle, ClickHouse 等)")
    private String datasourceType;

    @ApiModelProperty(value = "执行状态 (pending, running, success, failed)")
    private String status;

    @ApiModelProperty(value = "执行方式 (manual, schedule, auto_retry, dry_run)")
    private String executionMode;

    @ApiModelProperty(value = "关联采集任务 ID")
    private String collectorId;

    @ApiModelProperty(value = "开始时间起")
    private String startTimeBegin;

    @ApiModelProperty(value = "开始时间止")
    private String startTimeEnd;
}
