package com.yss.datamiddle.dqinsight.client.vo;

import com.yss.datamiddle.dqinsight.domain.model.HealthBand;
import com.yss.datamiddle.dqinsight.domain.model.HealthState;
import com.yss.datamiddle.dqinsight.domain.model.SourceTool;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 资产级 + 字段级健康分详情（冻结 OpenAPI AssetHealthDetail）。
 */
@Getter
@Setter
@NoArgsConstructor
public class AssetHealthDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 资产 ID */
    private String assetId;

    /** 资产名称 */
    private String assetName;

    /** 数据域 */
    private String domain;

    /** 资产类型 */
    private String assetType;

    /** 状态 */
    private HealthState state;

    /** 健康分 0~100 */
    private Integer score;

    /** 档位；无结果 / 过期为 null */
    private HealthBand band;

    /** 过期态字段 */
    private boolean expired;

    /** 最近结果时间（ISO 8601） */
    private String lastResultAt;

    /** 结果有效期至（ISO 8601） */
    private String validUntil;

    /** 规则通过率（如 '80%'） */
    private String passRate;

    /** 计算规则版本（如 v3），钻取页展示 */
    private String ruleVersion;

    /** 来源工具 */
    private SourceTool sourceTool;

    /** 字段级健康分列表 */
    private List<FieldHealthVO> fields;
}
