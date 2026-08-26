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
 * 元数据资产持久化对象（数据架构 asset 表，目录域）。
 *
 * <p>id 采用 VARCHAR(36) UUID（应用层生成，@TableId IdType.INPUT）；
 * source_id 为来源数据源 id；同数据源 + 名称唯一（幂等 upsert）。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("asset")
public class AssetPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("source_id")
    private String sourceId;

    @TableField("name")
    private String name;

    @TableField("type")
    private String type;

    @TableField("domain")
    private String domain;

    @TableField("owner")
    private String owner;

    @TableField("classification")
    private String classification;

    @TableField("database_name")
    private String databaseName;

    @TableField("source_system")
    private String sourceSystem;

    @TableField("collector_task_id")
    private String collectorTaskId;

    @TableField("is_excluded")
    private Boolean isExcluded;

    @TableField("status")
    private String status;

    @TableField("taint_status")
    private String taintStatus;

    @TableField("version")
    private String version;

    @TableField("description")
    private String description;

    @TableField("row_count")
    private Long rowCount;

    @TableField("storage_size")
    private String storageSize;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
