package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资产视图对象（冻结 OpenAPI 资产列表 data 元素）。
 *
 * <p>status 为资产状态（pending/claimed/archived/deleted）；favorite 为
 * 当前用户收藏状态；source 为数据源名称。</p>
 */
@Getter
@Setter
public class AssetVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 资产 id */
    private String id;

    /** 资产名称 */
    private String name;

    /** 资产类型（table/column/view） */
    private String type;

    /** 来源数据源 id */
    private String sourceId;

    /** 数据源名称 */
    private String source;

    /** 所属数据库名称 */
    private String databaseName;

    /** 所属 Schema 空间名称 */
    private String schemaName;

    /** 来源业务系统编码或名称 */
    private String sourceSystem;

    /** 关联采集任务 ID */
    private String collectorTaskId;

    /** 关联采集任务名称 */
    private String collectorName;

    /** 采集更新频率类型（定时/手动） */
    private String updateFrequency;

    /** 采集调度人类可读描述（如 每日, 04:11） */
    private String scheduleDescription;

    /** 最新版本号（如 V2026.08.23.221530） */
    private String version;

    /** 是否已剔除/软删除 */
    private Boolean isExcluded;

    /** 数据域 */
    private String domain;

    /** 负责人 */
    private String owner;

    /** 分级分类 */
    private String classification;

    /** 状态（pending/claimed/archived/deleted） */
    private String status;

    /** 当前用户是否收藏 */
    private Boolean favorite;

    /** 数据存疑状态：NORMAL / TAINTED */
    private String taintStatus;

    /** 质量健康分 */
    private Integer healthScore;

    /** 质量梯度：excellent / good / fair / poor */
    private String qualityBand;

    /** 数据源类型（如 MySQL / Oracle / PostgreSQL） */
    private String datasourceType;

    /** 元数据描述/表注释 */
    private String description;

    /** 表物理行数 */
    private Long rowCount;

    /** 表存储大小（如 12.03MB） */
    private String storageSize;

    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}
