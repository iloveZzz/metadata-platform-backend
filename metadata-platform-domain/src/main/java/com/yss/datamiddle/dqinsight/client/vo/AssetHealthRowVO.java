package com.yss.datamiddle.dqinsight.client.vo;

import com.yss.datamiddle.dqinsight.domain.model.HealthBand;
import com.yss.datamiddle.dqinsight.domain.model.HealthState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 资产级健康分行（冻结 OpenAPI AssetHealthRow，仪表盘 / 健康分列表）。
 *
 * <p>档位（优 / 良 / 差）与独立展示态字段（expired / validUntil）随行返回；过期态与「无结果」区分
 * （OQ-03 / SB-03 已确认）；noresult 由独立展示态表达（state=noresult / hasResult=false）。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class AssetHealthRowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 资产 ID */
    private String assetId;

    /** 资产名称 */
    private String assetName;

    /** 数据域 */
    private String domain;

    /** 资产类型 */
    private String assetType;

    /** 状态（ok=已计算档位；expired=过期独立展示态；noresult=无结果独立展示态；calculating=计算中） */
    private HealthState state;

    /** 健康分 0~100；noresult 时为 null */
    private Integer score;

    /** 档位；无结果 / 过期为 null */
    private HealthBand band;

    /** 过期态字段（独立展示态，与「无结果」区分；OQ-03 已确认默认 30 天） */
    private boolean expired;

    /** 是否已有结果（有 DQ 结果，含过期） */
    private boolean hasResult;

    /** 最近结果时间（ISO 8601） */
    private String lastResultAt;

    /** 结果有效期至（ISO 8601）；已失效时早于当前时间 */
    private String validUntil;

    /** 规则通过率（如 '80%'） */
    private String passRate;
}
