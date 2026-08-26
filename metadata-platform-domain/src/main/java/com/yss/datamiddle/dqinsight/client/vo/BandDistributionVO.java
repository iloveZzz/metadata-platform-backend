package com.yss.datamiddle.dqinsight.client.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 健康分分布（冻结 OpenAPI DashboardStats.bandDistribution）。
 *
 * <p>优 / 良 / 差为档位计数（不含过期行）；expired 为过期独立展示态计数；
 * noResult = 目标资产数 − 已接入数（钳制 ≥ 0，SB-07 口径）；无结果与过期不归入档位。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class BandDistributionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 档位 = 优 */
    private int good;

    /** 档位 = 良 */
    private int fair;

    /** 档位 = 差 */
    private int poor;

    /** 过期（独立展示态） */
    private int expired;

    /** 无结果（独立展示态） */
    private int noResult;
}
