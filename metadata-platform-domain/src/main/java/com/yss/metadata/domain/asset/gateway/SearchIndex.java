package com.yss.metadata.domain.asset.gateway;

import com.yss.metadata.client.dto.query.AssetSearchQuery;
import com.yss.metadata.domain.asset.model.AssetSearchResult;

/**
 * 资产发现索引端口（发现上下文；可替换 seam）。
 *
 * <p>当前实现为关系库 LIKE 检索（keyword 命中 asset.name 或 asset_column.name 时
 * 返回资产行；来源/类型/数据域/分类/收藏/我的资产筛选；sort 枚举
 * [updatedAt,name,classification] 默认 updatedAt 倒序；分页；0 条以空分页表达）。
 * 百万级 P95&lt;1s 性能目标随规模评估，可替换为 ES 等外部索引。</p>
 */
public interface SearchIndex {

    /**
     * 资产目录搜索。
     *
     * @param query 搜索条件（分页/筛选/排序/当前用户）
     * @return 分页结果（0 条以空分页表达，非错误）
     */
    AssetSearchResult search(AssetSearchQuery query);
}
