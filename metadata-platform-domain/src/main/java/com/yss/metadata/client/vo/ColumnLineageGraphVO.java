package com.yss.metadata.client.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 字段级血缘图谱视图对象 (ColumnLineageGraphVO)。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnLineageGraphVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 中心聚焦资产 ID */
    private String centerAssetId;

    /** 当前选中的聚焦字段 ID (可选) */
    private String centerColumnId;

    /** 图谱中的字段节点列表 */
    private List<ColumnLineageNodeVO> nodes;

    /** 字段间的血缘边列表 */
    private List<LineageEdgeVO> edges;
}
