package com.yss.metadata.domain.asset.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 资产搜索分页结果（发现上下文 SearchIndex 端口返回值）。
 *
 * <p>0 条以空分页表达（total=0，items 为空列表），非错误。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetSearchResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页资产（含当前用户收藏状态等查询组合字段） */
    private List<Asset> items;

    /** 命中总数 */
    private long total;

    /** 当前页码（从 1 起） */
    private int pageIndex;

    /** 每页大小 */
    private int pageSize;
}
