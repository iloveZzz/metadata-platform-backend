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

/**
 * 资产列（明细）持久化对象（数据架构 asset_column 表）。
 *
 * <p>以 asset_id 归属资产；列清单全量替换（采集幂等覆盖语义）。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("asset_column")
public class AssetColumnPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("asset_id")
    private String assetId;

    @TableField("name")
    private String name;

    @TableField("type")
    private String type;

    @TableField("comment")
    private String comment;

    @TableField("pk")
    private Boolean pk;

    @TableField("ordinal_position")
    private Integer ordinalPosition;

    @TableField("classification")
    private String classification;
}
