package com.yss.metadata.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 资产标签持久化对象（数据架构 asset_tag 表；切片 02 新增）。
 *
 * <p>复合主键 (asset_id, tag)（无单列主键）；标签覆盖式更新
 * （PUT /tags 全量替换）。</p>
 */
@Getter
@Setter
@TableName("asset_tag")
public class AssetTagPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 资产 id（复合主键） */
    @TableField("asset_id")
    private String assetId;

    /** 标签（复合主键，varchar(64)） */
    @TableField("tag")
    private String tag;
}
