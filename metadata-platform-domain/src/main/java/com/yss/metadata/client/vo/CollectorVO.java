package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 采集任务视图对象（冻结 OpenAPI Collector 响应 data）。
 */
@Getter
@Setter
public class CollectorVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 采集任务 id */
    private String id;

    /** 任务名称 */
    private String name;

    /** 目标数据源（连接器 id） */
    private String connectorId;

    /** 调度（cron 或周期描述） */
    private String schedule;

    /** 采集模式（incremental/full） */
    private String mode;

    /** 覆盖策略（ignore/overwrite/abort-on-failure） */
    private String strategy;

    /** 是否自动识别分类 */
    private Boolean autoClassify;

    /** 状态（pending/running/success/failed/cancelled） */
    private String status;

    /** 失败原因 */
    private String failReason;

    /** 最近一次执行开始时间（ISO 格式） */
    private String lastRunAt;

    /** 负责人（工号/用户ID） */
    private String owner;

    /** 任务描述 */
    private String description;

    /** 生效状态（true 生效 / false 停用） */
    private Boolean enabled;

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

    /** 创建时间（ISO 格式） */
    private String createdAt;

    /** 最后更新时间（ISO 格式） */
    private String updatedAt;
}
