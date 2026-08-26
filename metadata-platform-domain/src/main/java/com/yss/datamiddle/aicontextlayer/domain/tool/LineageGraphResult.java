package com.yss.datamiddle.aicontextlayer.domain.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 血缘拓扑图响应结果（契约 3.3）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineageGraphResult implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int MAX_NODES = 500;
    public static final int MAX_EDGES = 1000;

    private String assetId;
    private List<LineageNode> nodes;
    private List<LineageEdge> edges;
}
