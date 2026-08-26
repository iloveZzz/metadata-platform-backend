package com.yss.datamiddle.dqinsight.client.dto.query;

import com.yss.cloud.dto.page.PageQuery;
import com.yss.datamiddle.dqinsight.domain.model.BandFilter;
import com.yss.datamiddle.dqinsight.domain.model.DashboardSort;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 资产级健康分列表查询（GET /api/dq/health 与 GET /api/dq/dashboard 资产列表共用）。
 *
 * <p>health：assetId 精确跳转 / domain / band / assetType 筛选 + 分页（sort 为 null → 默认最近结果倒序，
 * 冻结契约 m3）；dashboard：追加 sort（score / lastResultAt / name，默认 score）与 visibleDomains
 * （DataDomainFilter seam，C24 域过滤）。band 含独立展示态筛选（expired / noresult，与档位并列，
 * BandFilter）；0 条以空分页表达。</p>
 */
@Getter
@Setter
public class HealthScorePageQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 资产 ID 精确跳转 */
    private String assetId;

    /** 数据域筛选 */
    private String domain;

    /** 档位或独立展示态（expired / noresult）筛选 */
    private BandFilter band;

    /** 资产类型筛选 */
    private String assetType;

    /** 排序字段（null = 默认最近结果时间倒序；仪表盘默认 score，C28） */
    private DashboardSort sort;

    /** 当前用户可见数据域（DataDomainFilter seam；null / 空 = 不做域限制） */
    private List<String> visibleDomains;
}
