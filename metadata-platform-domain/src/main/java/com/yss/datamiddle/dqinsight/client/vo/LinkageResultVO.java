package com.yss.datamiddle.dqinsight.client.vo;

import com.yss.datamiddle.dqinsight.domain.model.LinkageMatchMode;
import com.yss.datamiddle.dqinsight.domain.model.LinkageState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 人工映射结果（冻结 OpenAPI LinkageResult：data = 已关联后的 AssetLinkage 快照；
 * 健康分首次计算已触发）。
 */
@Getter
@Setter
@NoArgsConstructor
public class LinkageResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关联记录 ID */
    private String id;

    /** 已关联的资产 ID（主平台口径） */
    private String assetId;

    /** 资产名称快照 */
    private String assetName;

    /** 数据域快照 */
    private String domain;

    /** 资产类型快照 */
    private String assetType;

    /** 关联状态（linked） */
    private LinkageState state;

    /** 匹配方式（manual） */
    private LinkageMatchMode matchMode;

    /** 映射时间（ISO 8601） */
    private String mappedAt;
}
