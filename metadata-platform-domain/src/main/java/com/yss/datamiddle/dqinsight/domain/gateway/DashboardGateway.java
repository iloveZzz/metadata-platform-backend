package com.yss.datamiddle.dqinsight.domain.gateway;

import com.yss.datamiddle.dqinsight.client.dto.query.HealthScorePageQuery;
import com.yss.datamiddle.dqinsight.client.vo.DashboardStatsVO;

/**
 * 仪表盘聚合端口（Domain 定义，Infrastructure 实现）。
 *
 * <p>只读查询投影（CQRS，数据架构 §7）：bandDistribution / 已接入 / 低分 / 覆盖率聚合
 * 基于数据域内可见资产全集（DataDomainFilter seam + domain / assetType 筛选，不含 band 筛选——
 * 档位筛选仅作用于资产列表）；覆盖率分母 targetAssetCount 来自防腐层拉取主平台口径。
 * 聚合领域规则（SB-07）在 {@link DashboardStatsVO#compute}。</p>
 */
public interface DashboardGateway {

    /**
     * 仪表盘聚合统计（数据域内可见资产全集口径；0 条以空分布表达，非错误）。
     */
    DashboardStatsVO loadStats(HealthScorePageQuery query);
}
