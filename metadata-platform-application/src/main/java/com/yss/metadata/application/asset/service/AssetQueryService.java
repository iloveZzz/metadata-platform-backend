package com.yss.metadata.application.asset.service;

import com.yss.metadata.client.dto.query.AssetSearchQuery;
import com.yss.metadata.client.vo.AssetDetailVO;
import com.yss.metadata.client.vo.AssetPageVO;

/**
 * 资产查询应用服务（WU-02-01 目录搜索 / WU-02-03 详情聚合）。
 *
 * <p>搜索经发现上下文 SearchIndex 端口（关系库 LIKE，可替换 seam）；
 * 详情聚合元数据 + 字段清单 + 版本/变更记录 + 标签 + 当前用户收藏状态。</p>
 */
public interface AssetQueryService {

    /**
     * 资产目录搜索（列级命中/筛选/排序/分页；0 条以空分页表达，非错误）。
     */
    AssetPageVO search(AssetSearchQuery query);

    /**
     * 资产详情聚合（404 语义：资产不存在）。
     *
     * @param id            资产 id
     * @param currentUserId 当前用户（收藏状态；RBAC slice 06 前 seam）
     */
    AssetDetailVO getDetail(String id, String currentUserId);
}
