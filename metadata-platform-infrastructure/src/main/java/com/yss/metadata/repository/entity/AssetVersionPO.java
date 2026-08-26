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
 * 资产版本持久化对象（数据架构 asset_version 表，变更快照可回溯）。
 *
 * <p>每轮采集成功生成新版本快照：version 递增（首版 1），
 * schema_diff 记录列快照（变更内容），同资产 + 版本唯一。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("asset_version")
public class AssetVersionPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("asset_id")
    private String assetId;

    @TableField("version")
    private Integer version;

    @TableField("schema_diff")
    private String schemaDiff;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
