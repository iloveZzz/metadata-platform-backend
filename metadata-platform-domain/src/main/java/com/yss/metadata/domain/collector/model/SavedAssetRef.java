package com.yss.metadata.domain.collector.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 已入库资产引用（saveAssets 返回：资产 id + 名称 + 列引用）。
 *
 * <p>切片 04 新增返回类型（new_impact 登记）：供采集编排在 autoClassify 时
 * 以已解析的资产/列 id 运行敏感识别（识别候选需挂载 id）。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedAssetRef implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 已入库资产 id */
    private String assetId;

    /** 资产名称（同数据源内唯一，upsert 键） */
    private String name;

    /** 已入库列引用（与采集列同序） */
    private List<SavedColumnRef> columns = new ArrayList<>();
}
