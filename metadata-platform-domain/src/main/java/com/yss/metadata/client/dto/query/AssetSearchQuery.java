package com.yss.metadata.client.dto.query;

import com.yss.cloud.dto.page.PageQuery;
import lombok.Getter;
import lombok.Setter;

/**
 * 资产搜索查询（冻结 OpenAPI GET /api/assets 查询参数）。
 *
 * <p>继承 {@link PageQuery}（读操作 QueryDTO 约定）；请求参数 page/size 经
 * {@link #setPage(int)} / {@link #setSize(int)} 映射到 pageIndex/pageSize
 * （OpenAPI 默认 size=20，上限 200）。</p>
 *
 * <p>当前用户上下文 seam（slice 06 RBAC 替换）：favorite/mine 筛选基于
 * {@link #currentUserId}，由 Web 层从请求头 X-User-Id 解析（缺省 default-user）。</p>
 */
@Getter
@Setter
public class AssetSearchQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 每页上限（冻结 OpenAPI size maximum: 200） */
    public static final int MAX_PAGE_SIZE = 200;

    /** 默认每页大小（冻结 OpenAPI size default: 20） */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 关键词（命中资产名称或字段名称，列级命中） */
    private String keyword;

    /** 数据源筛选（按数据源名称） */
    private String source;

    /** 数据源 ID 筛选 */
    private String sourceId;

    /** 数据库 / Schema 空间筛选 */
    private String database;

    /** 来源业务系统编码或名称筛选 */
    private String sourceSystem;

    /** 关联采集任务 ID 筛选 */
    private String collectorTaskId;

    /** 是否查询已剔除/软删除数据（true: 仅看已剔除数据；false/null: 仅看正常数据） */
    private Boolean isExcluded;

    /** 资产类型筛选（table/column） */
    private String type;

    /** 数据域筛选 */
    private String domain;

    /** 分级分类筛选 */
    private String classification;

    /** 仅看收藏（true 生效） */
    private Boolean favorite;

    /** 我的资产（owner=当前用户，true 生效） */
    private Boolean mine;

    /** 排序字段（updatedAt/name/classification，默认 updatedAt 倒序） */
    private String sort;

    /** 当前用户（Web 层解析 X-User-Id，缺省 default-user；seam，slice 06 替换） */
    private String currentUserId;

    /** 允许访问的数据域（slice 06 RBAC：Web 层解析 X-User-Domains 头；null=全部放行） */
    private java.util.List<String> allowedDomains;

    public AssetSearchQuery() {
        setPageIndex(1);
        setPageSize(DEFAULT_PAGE_SIZE);
    }

    /**
     * 请求参数 page → pageIndex（OpenAPI page 语义，从 1 起；钳制 ≥1）。
     */
    public void setPage(int page) {
        setPageIndex(Math.max(1, page));
    }

    /**
     * 请求参数 size → pageSize（OpenAPI size 语义；钳制 1 ≤ size ≤ 200）。
     */
    public void setSize(int size) {
        setPageSize(Math.max(1, Math.min(size, MAX_PAGE_SIZE)));
    }

    @Override
    public int getPageSize() {
        return Math.min(super.getPageSize(), MAX_PAGE_SIZE);
    }
}
