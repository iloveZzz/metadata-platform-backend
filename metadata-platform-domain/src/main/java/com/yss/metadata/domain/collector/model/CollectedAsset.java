package com.yss.metadata.domain.collector.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 采集产物资产值对象（数据架构 asset / asset_column 表，采集上下文产出）。
 *
 * <p>由采集执行 SPI 产出，经 AssetGateway 幂等入库（同数据源 + 名称 upsert，
 * 列全量替换），并生成资产版本快照。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectedAsset implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 资产名称（表/视图名等，同数据源内唯一） */
    private String name;

    /** 资产类型：table / column / view */
    private String type;

    /** 数据域 */
    private String domain;

    /** 负责人 */
    private String owner;

    /** 分级分类 */
    private String classification;

    /** 所属数据库名称 */
    private String databaseName;

    /** 所属 Schema 空间名称 */
    private String schemaName;

    /** 来源业务系统编码或名称 */
    private String sourceSystem;

    /** 关联采集任务 ID */
    private String collectorTaskId;

    /** 采集版本号（可选，若为空则在入库时由网关生成） */
    private String version;

    /** 元数据描述/表注释 */
    private String description;

    /** 表物理行数 */
    private Long rowCount;

    /** 表存储大小（如 12.03MB） */
    private String storageSize;

    /** 列明细 */
    @Builder.Default
    private List<CollectedColumn> columns = new ArrayList<>();
}
