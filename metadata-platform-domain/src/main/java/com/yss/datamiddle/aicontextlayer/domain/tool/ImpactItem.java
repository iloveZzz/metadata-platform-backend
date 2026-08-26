package com.yss.datamiddle.aicontextlayer.domain.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 影响分析单项（SEC-02 / SEC-04）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImpactItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String assetId;
    private String name;
    private String type;
    private String domain;
    private String classification;
    private int depth;
    private Provenance provenance;
}
