package com.yss.metadata.domain.lineage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 血缘边实体（数据架构 LineageEdge：from_asset/to_asset/type/confidence/remark/graph_version）。
 *
 * <p>方向语义：from=上游（源），to=下游（目标）；图版本 graphVersion 为
 * 并发防冲突 token（乐观锁，见 {@link LineageGraph#ensureVersion(String)}）。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineageEdge implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（UUID） */
    private String id;

    /** 上游资产 id */
    private String fromAssetId;

    /** 下游资产 id */
    private String toAssetId;

    /** 上游字段 id（可选，字段级血缘） */
    private String fromColumnId;

    /** 下游字段 id（可选，字段级血缘） */
    private String toColumnId;

    /** 字段转换 SQL 表达式 */
    private String transformExpr;

    /** 表达式类型：DIRECT/COMPUTED/AGGREGATE/MANUAL */
    private String exprType;

    /** 血缘类型（sql/job/manual） */
    private LineageType type;

    /** 置信度（auto-high/auto-mid/manual-high/low） */
    private LineageConfidence confidence;

    /** 备注（补录依据等） */
    private String remark;

    /** 图版本 token（乐观锁） */
    private String graphVersion;

    /**
     * 环检测用：冲突边文本描述（from→to）。
     */
    public String describe() {
        if (fromColumnId != null && toColumnId != null) {
            return fromAssetId + "." + fromColumnId + "→" + toAssetId + "." + toColumnId;
        }
        return fromAssetId + "→" + toAssetId;
    }
}
