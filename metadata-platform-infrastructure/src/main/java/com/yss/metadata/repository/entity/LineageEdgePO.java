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
 * 血缘边持久化对象（lineage_edge 邻接表）。
 *
 * <p>from_asset=上游，to_asset=下游；graph_version 为图版本 token
 * （并发防冲突乐观锁，应用层 UUID 推进）。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("lineage_edge")
public class LineageEdgePO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("from_asset")
    private String fromAsset;

    @TableField("to_asset")
    private String toAsset;

    @TableField("from_column_id")
    private String fromColumnId;

    @TableField("to_column_id")
    private String toColumnId;

    @TableField("transform_expr")
    private String transformExpr;

    @TableField("expr_type")
    private String exprType;

    @TableField("type")
    private String type;

    @TableField("confidence")
    private String confidence;

    @TableField("remark")
    private String remark;

    @TableField("graph_version")
    private String graphVersion;
}
