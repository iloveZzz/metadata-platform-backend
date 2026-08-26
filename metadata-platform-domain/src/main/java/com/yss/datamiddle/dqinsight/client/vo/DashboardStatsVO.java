package com.yss.datamiddle.dqinsight.client.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 仪表盘聚合统计（冻结 OpenAPI DashboardStats）。
 *
 * <p>覆盖率口径（SB-07 已确认）：已接入（有 DQ 结果，含过期）资产数 ÷ 数据域内可见目标资产数 × 100%；
 * 目标资产数来自防腐层拉取主平台口径（数据架构 §7）。noResult 独立展示态 = 目标资产数 − 已接入数
 * （钳制 ≥ 0）；targetAssetCount 为 0（防腐层不可用 / 无目标资产）时覆盖率按 0 表达（不除零）。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class DashboardStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 健康分分布（优 / 良 / 差 + 过期 / 无结果独立展示态） */
    private BandDistributionVO bandDistribution;

    /** 已接入资产数（有 DQ 结果，含过期） */
    private int ingestedAssetCount;

    /** 低分资产数（档位 = 差，不含过期行） */
    private int lowScoreAssetCount;

    /** 数据域内可见目标资产数（防腐层拉取主平台口径） */
    private int targetAssetCount;

    /** 覆盖率 % = 已接入（含过期）÷ 数据域内可见目标资产 × 100（SB-07） */
    private float coverage;

    /**
     * 聚合装配（领域规则，SB-07 口径）。
     *
     * @param good 档位 = 优（非过期）
     * @param fair 档位 = 良（非过期）
     * @param poor 档位 = 差（非过期）
     * @param expired 过期独立展示态计数（有结果但已过期，计入已接入）
     * @param lowScore 低分资产数（档位 = 差，不含过期行）
     * @param targetAssetCount 数据域内可见目标资产数（防腐层）
     */
    public static DashboardStatsVO compute(int good, int fair, int poor, int expired,
            int lowScore, int targetAssetCount) {
        int ingested = good + fair + poor + expired;
        int noResult = Math.max(0, targetAssetCount - ingested);
        float coverage = targetAssetCount <= 0 ? 0f : ingested * 100f / targetAssetCount;

        BandDistributionVO distribution = new BandDistributionVO();
        distribution.setGood(good);
        distribution.setFair(fair);
        distribution.setPoor(poor);
        distribution.setExpired(expired);
        distribution.setNoResult(noResult);

        DashboardStatsVO stats = new DashboardStatsVO();
        stats.setBandDistribution(distribution);
        stats.setIngestedAssetCount(ingested);
        stats.setLowScoreAssetCount(lowScore);
        stats.setTargetAssetCount(targetAssetCount);
        stats.setCoverage(coverage);
        return stats;
    }
}
