package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yss.datamiddle.dqinsight.client.dto.query.HealthScorePageQuery;
import com.yss.datamiddle.dqinsight.client.vo.DashboardStatsVO;
import com.yss.datamiddle.dqinsight.domain.gateway.CatalogAclGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.DashboardGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.DataDomainFilter;
import com.yss.datamiddle.dqinsight.repository.DqHealthScoreRepository;
import com.yss.datamiddle.dqinsight.repository.entity.DqHealthScorePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘聚合实现（只读查询投影，数据架构 §7 / C28 聚合索引覆盖）。
 *
 * <p>bandDistribution / 已接入 / 低分基于 dq_health_score 资产级行（field_name IS NULL）聚合，
 * 过期由 valid_until &lt; CURRENT_TIMESTAMP 派生（C23 与「无结果」独立展示态不混淆）；
 * 统计口径 = 数据域内可见资产全集（DataDomainFilter seam + domain / assetType 筛选，不含 band——
 * 档位筛选仅作用于资产列表）；覆盖率分母 targetAssetCount 来自防腐层（SB-07）；
 * 聚合领域规则（noResult / coverage）在 {@link DashboardStatsVO#compute}。</p>
 */
@Repository
@RequiredArgsConstructor
public class DashboardGatewayImpl implements DashboardGateway {

    private static final String BUCKET_EXPIRED = "expired";

    /** SQL DATETIME 字面量格式（H2 MySQL 模式 / MySQL） */
    private static final DateTimeFormatter SQL_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DqHealthScoreRepository dqHealthScoreRepository;
    private final DataDomainFilter dataDomainFilter;
    private final CatalogAclGateway catalogAclGateway;

    @Override
    public DashboardStatsVO loadStats(HealthScorePageQuery query) {
        // 数据域可见范围（C24 seam；控制器已设置时保留，否则回退端口解析）
        if (query.getVisibleDomains() == null || query.getVisibleDomains().isEmpty()) {
            List<String> visible = dataDomainFilter.visibleDomains();
            query.setVisibleDomains(visible == null ? Collections.emptyList() : visible);
        }

        int good = 0;
        int fair = 0;
        int poor = 0;
        int expired = 0;
        for (Map<String, Object> row : bandBucketRows(query)) {
            String bucket = findIgnoreCase(row, "bucket");
            Number cnt = findNumberIgnoreCase(row, "cnt");
            if (bucket == null || cnt == null) {
                continue;
            }
            int value = cnt.intValue();
            switch (bucket) {
                case "优":
                    good += value;
                    break;
                case "良":
                    fair += value;
                    break;
                case "差":
                    poor += value;
                    break;
                case BUCKET_EXPIRED:
                    expired += value;
                    break;
                default:
                    break;
            }
        }
        int lowScore = lowScoreCount(query);
        int target = catalogAclGateway.countVisibleTargetAssets(query.getDomain());
        return DashboardStatsVO.compute(good, fair, poor, expired, lowScore, target);
    }

    /**
     * bandDistribution 桶计数：过期由 valid_until &lt; 查询时刻派生（C23），与档位并列。
     */
    private List<Map<String, Object>> bandBucketRows(HealthScorePageQuery query) {
        // 查询时刻由服务端生成并格式化为 SQL 字面量（非用户输入；H2 MySQL 模式 / MySQL 均接受
        // 'yyyy-MM-dd HH:mm:ss' 与 DATETIME 比较），与行级过期派生（Instant.now()）同刻口径
        String nowLiteral = SQL_DATETIME.format(LocalDateTime.now());
        QueryWrapper<DqHealthScorePO> wrapper = new QueryWrapper<>();
        wrapper.select("CASE WHEN valid_until < '" + nowLiteral + "' THEN '" + BUCKET_EXPIRED
                        + "' ELSE band END AS bucket", "COUNT(*) AS cnt")
                .isNull("field_name")
                .groupBy("bucket");
        applyScope(wrapper, query);
        return dqHealthScoreRepository.selectMaps(wrapper);
    }

    /**
     * 低分资产数（档位 = 差，不含过期行）。
     */
    private int lowScoreCount(HealthScorePageQuery query) {
        QueryWrapper<DqHealthScorePO> wrapper = new QueryWrapper<>();
        LocalDateTime now = LocalDateTime.now();
        wrapper.select("COUNT(*) AS cnt")
                .isNull("field_name")
                .eq("band", "差")
                .and(w -> w.isNull("valid_until").or().ge("valid_until", now));
        applyScope(wrapper, query);
        List<Map<String, Object>> rows = dqHealthScoreRepository.selectMaps(wrapper);
        Number cnt = rows.isEmpty() ? null : findNumberIgnoreCase(rows.get(0), "cnt");
        return cnt == null ? 0 : cnt.intValue();
    }

    /**
     * 统计口径范围：数据域内可见资产全集（domain / assetType 筛选 + DataDomainFilter 可见域；
     * 不含 band——档位筛选仅作用于资产列表）。
     */
    private void applyScope(QueryWrapper<DqHealthScorePO> wrapper, HealthScorePageQuery query) {
        if (query.getDomain() != null && !query.getDomain().trim().isEmpty()) {
            wrapper.eq("domain", query.getDomain().trim());
        }
        if (query.getAssetType() != null && !query.getAssetType().trim().isEmpty()) {
            wrapper.eq("asset_type", query.getAssetType().trim());
        }
        if (query.getVisibleDomains() != null && !query.getVisibleDomains().isEmpty()) {
            wrapper.in("domain", query.getVisibleDomains());
        }
    }

    private static String findIgnoreCase(Map<String, Object> row, String key) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key) && entry.getValue() != null) {
                return String.valueOf(entry.getValue());
            }
        }
        return null;
    }

    private static Number findNumberIgnoreCase(Map<String, Object> row, String key) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)
                    && entry.getValue() instanceof Number) {
                return (Number) entry.getValue();
            }
        }
        return null;
    }
}
