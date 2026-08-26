package com.yss.datamiddle.aicontextlayer.domain.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 影响分析响应结果（契约 3.4）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImpactAnalysisResult implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int MAX_IMPACT_ITEMS = 500;

    private String rootAssetId;
    private int totalCount;
    private List<ImpactDepthGroup> depthGroups;
}
