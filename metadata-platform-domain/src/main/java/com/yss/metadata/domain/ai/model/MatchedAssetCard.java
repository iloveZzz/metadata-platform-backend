package com.yss.metadata.domain.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 智能找数推荐资产卡片（VO）
 *
 * @author ai
 * @since 2026-08-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchedAssetCard implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 资产ID
     */
    private String assetId;

    /**
     * 资产英文名
     */
    private String assetName;

    /**
     * 资产中文描述 / 标题
     */
    private String title;

    /**
     * 所属数据域
     */
    private String domain;

    /**
     * 匹配置信度文案（如 "极高匹配 (96%)"）
     */
    private String confidenceText;

    /**
     * 匹配原因 / 解释
     */
    private String matchReason;

    /**
     * 质量健康分（0~100）
     */
    private Integer healthScore;

    /**
     * 质量等级（excellent / good / fair / poor）
     */
    private String qualityBand;

    /**
     * 数据污染 / 存疑状态（NORMAL / TAINTED）
     */
    private String taintStatus;
}
