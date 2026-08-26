package com.yss.datamiddle.dqinsight.client.vo;

import com.yss.cloud.dto.result.PageResult;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 仪表盘聚合响应（冻结 OpenAPI Dashboard：data = { stats: DashboardStats, assets: 分页 AssetHealthRow[] }）。
 *
 * <p>信封为 YSS SingleResult（data 为对象）；assets 为嵌套 PageResult（分页元数据 totalCount /
 * pageIndex / pageSize 在 assets 内，data 为 AssetHealthRow[] 数组，A3-AM-01 信封形态）；
 * 0 条以空分页表达（totalCount=0、data=[]，非错误，C28）。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class DashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 聚合统计 */
    private DashboardStatsVO stats;

    /** 资产健康分列表（分页 / 筛选 / 排序） */
    private PageResult<AssetHealthRowVO> assets;
}
