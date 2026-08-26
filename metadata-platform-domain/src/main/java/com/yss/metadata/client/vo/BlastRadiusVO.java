package com.yss.metadata.client.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 下游爆炸半径分析报告 VO (GET /api/dq/assets/{id}/blast-radius)
 *
 * @author ai
 * @since 2026-08-15
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlastRadiusVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String originAssetId;
    private String originAssetName;
    private List<BlastRadiusAssetVO> impactedAssets;
    private Integer totalImpactedCount;
    private Integer maxDepth;
    private List<String> impactedDomains;
}
