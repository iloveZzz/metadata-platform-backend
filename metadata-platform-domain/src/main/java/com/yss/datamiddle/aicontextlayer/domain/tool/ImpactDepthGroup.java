package com.yss.datamiddle.aicontextlayer.domain.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 影响分析按深度分组（契约 3.4）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImpactDepthGroup implements Serializable {
    private static final long serialVersionUID = 1L;

    private int depth;
    private int groupCount;
    private List<ImpactItem> items;
}
