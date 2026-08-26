package com.yss.metadata.domain.dq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 下游爆炸半径评估报告聚合模型
 *
 * @author ai
 * @since 2026-08-15
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlastRadiusReport implements Serializable {
    private static final long serialVersionUID = 1L;

    private String originAssetId;
    private String originAssetName;
    private List<BlastRadiusAsset> impactedAssets;
    private Integer totalImpactedCount;
    private Integer maxDepth;
    private List<String> impactedDomains;
}
