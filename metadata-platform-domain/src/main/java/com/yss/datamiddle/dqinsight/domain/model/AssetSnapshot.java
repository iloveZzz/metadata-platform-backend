package com.yss.datamiddle.dqinsight.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 资产快照（防腐层消费冻结资产 API 冗余的快照字段，数据架构 §10）。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 资产 ID（主平台口径） */
    private String assetId;

    /** 资产名称 */
    private String assetName;

    /** 数据域 */
    private String domain;

    /** 资产类型 */
    private String assetType;
}
