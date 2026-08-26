package com.yss.metadata.application.lineage.service;

import com.yss.metadata.client.vo.LineageGraphVO;

/**
 * 血缘图谱查询应用服务（WU-03-01 图谱查询）。
 *
 * <p>按资产查询血缘邻域（confidence 筛选，all/缺省不过滤）；
 * 空血缘以空结构表达（非错误）；资产不存在抛 404。</p>
 */
public interface LineageQueryService {

    /**
     * 血缘图谱（edges + graphVersionToken）。
     *
     * @param assetId    中心资产 id
     * @param confidence 置信度筛选（auto-high/auto-mid/manual-high/low/all，默认 all）
     */
    LineageGraphVO getGraph(String assetId, String confidence);
}
