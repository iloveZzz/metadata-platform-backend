package com.yss.metadata.client.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作流节点视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "工作流节点视图对象")
public class WorkflowNodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "节点 ID")
    private String id;

    @ApiModelProperty(value = "节点名称")
    private String name;

    @ApiModelProperty(value = "节点类型 (dlink, jdbc_probe, schema_parse, catalog_ingest)")
    private String type;

    @ApiModelProperty(value = "节点类型描述")
    private String typeDescription;

    @ApiModelProperty(value = "执行状态 (pending, running, success, failed)")
    private String status;

    @ApiModelProperty(value = "执行状态描述")
    private String statusDescription;

    @ApiModelProperty(value = "开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "耗时 (毫秒)")
    private Long durationMs;

    @ApiModelProperty(value = "日志流")
    private List<String> logs;

    @ApiModelProperty(value = "异常堆栈信息")
    private String exceptionInfo;

    @ApiModelProperty(value = "性能诊断指标")
    private Map<String, Object> performanceMetrics;

    @ApiModelProperty(value = "节点执行参数")
    private Map<String, Object> parameters;

    @ApiModelProperty(value = "智能排障与诊断建议")
    private Map<String, Object> diagnosisAdvice;

    @ApiModelProperty(value = "运行代码 (SQL / Flink 脚本)")
    private String executedCode;
}
