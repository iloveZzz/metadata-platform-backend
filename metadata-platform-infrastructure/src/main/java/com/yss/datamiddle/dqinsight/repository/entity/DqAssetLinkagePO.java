package com.yss.datamiddle.dqinsight.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 资产关联持久化对象（dq_asset_linkage；pending 队列 = state = pending；UNIQUE(batch_id, source_asset_id)）。
 */
@Getter
@Setter
@TableName("dq_asset_linkage")
public class DqAssetLinkagePO {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 批次 ID */
    @TableField("batch_id")
    private Long batchId;

    /** 源资产 ID（结果中的资产 ID） */
    @TableField("source_asset_id")
    private String sourceAssetId;

    /** 解析后资产 ID（pending 时为 null） */
    @TableField("resolved_asset_id")
    private String resolvedAssetId;

    /** 资产名称快照 */
    @TableField("asset_name")
    private String assetName;

    /** 数据域快照 */
    @TableField("domain")
    private String domain;

    /** 资产类型快照 */
    @TableField("asset_type")
    private String assetType;

    /** 匹配方式（auto / manual） */
    @TableField("match_mode")
    private String matchMode;

    /** 关联状态（linked / pending） */
    @TableField("state")
    private String state;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 映射时间 */
    @TableField("mapped_at")
    private LocalDateTime mappedAt;

    /** 映射人 */
    @TableField("mapped_by")
    private String mappedBy;

    /** 备注 */
    @TableField("note")
    private String note;
}
