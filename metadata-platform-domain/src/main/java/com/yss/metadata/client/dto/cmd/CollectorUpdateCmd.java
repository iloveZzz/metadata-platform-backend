package com.yss.metadata.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;
import com.yss.metadata.domain.collector.model.CollectorMode;
import com.yss.metadata.domain.collector.model.CollectorStrategy;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 编辑采集任务调度命令（冻结 OpenAPI PUT /api/collectors/{id}）。
 */
@Getter
@Setter
public class CollectorUpdateCmd extends CommandDTO {

    private static final long serialVersionUID = 1L;

    /** 采集任务 id */
    @NotBlank(message = "采集任务 id 不能为空")
    private String id;

    /** 任务名称 */
    @NotBlank(message = "任务名称不能为空")
    private String name;

    /** 目标数据源（连接器 id） */
    @NotBlank(message = "目标数据源不能为空")
    private String connectorId;

    /** 调度（cron 或周期描述） */
    @NotBlank(message = "调度不能为空")
    private String schedule;

    /** 采集模式 */
    @NotNull(message = "采集模式不能为空")
    private CollectorMode mode;

    /** 覆盖策略 */
    private CollectorStrategy strategy = CollectorStrategy.IGNORE;

    /** 是否自动识别分类（默认 true） */
    private Boolean autoClassify = Boolean.TRUE;

    /** 负责人（工号/用户ID） */
    private String owner;

    /** 任务描述 */
    private String description;

    /** 生效状态（默认 true） */
    private Boolean enabled = Boolean.TRUE;

    /** 数据源类型（MySQL, Oracle, ClickHouse 等） */
    private String datasourceType;

    /** 业务来源系统 */
    private String sourceSystem;

    /** 采集范围（all / custom） */
    private String scopeType;

    /** 指定 Database */
    private String selectedDatabases;

    /** 是否开启失败重试 */
    private Boolean retryEnabled;

    /** 重试次数 */
    private Integer retryCount;

    /** 重试间隔（分钟） */
    private Integer retryInterval;
}
