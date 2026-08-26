package com.yss.datamiddle.dqinsight.domain.gateway;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.dqinsight.client.dto.query.HealthScorePageQuery;
import com.yss.datamiddle.dqinsight.client.vo.AssetHealthDetailVO;
import com.yss.datamiddle.dqinsight.client.vo.AssetHealthRowVO;
import com.yss.datamiddle.dqinsight.client.vo.RuleDetailVO;
import com.yss.datamiddle.dqinsight.domain.model.HealthScore;
import com.yss.datamiddle.dqinsight.domain.model.RuleScoreSnapshot;

import java.util.List;

/**
 * 健康分仓储端口（Domain 定义，Infrastructure 实现）。
 *
 * <p>写：dq_health_score upsert（(asset_id, field_name) 唯一保留最新）+ dq_rule_detail 快照单聚合事务
 * （计算幂等可重算，数据架构 §7）；读：health / details 查询投影（过期态由查询派生）。</p>
 */
public interface HealthScoreGateway {

    /**
     * 健康分 upsert（存在则更新、否则插入；返回带主键的聚合）。
     */
    HealthScore upsert(HealthScore score);

    /**
     * 规则明细快照写入（按 (batch_id, asset_id, field_name, rule_name) 唯一键 upsert，幂等可重算）。
     */
    void saveRuleDetails(Long batchId, String assetId, String fieldName, List<RuleScoreSnapshot> details);

    /**
     * 查询最新计算规则版本（无历史计算返回 null）。
     */
    String findLatestRuleVersion(String assetId, String fieldName);

    /**
     * 资产级健康分列表（field_name IS NULL；档位 / 独立展示态筛选 + 分页；PageQuery 自动分页，
     * 总数经 query.tempTotalCount 回读；0 条以空分页表达）。
     */
    List<AssetHealthRowVO> listAssetHealth(HealthScorePageQuery query);

    /**
     * 资产级 + 字段级健康分详情（无资产级健康分返回 null → 404 err.dq.not-found）。
     */
    AssetHealthDetailVO findAssetHealthDetail(String assetId);

    /**
     * 规则明细钻取（fieldName 为空 = 资产级；无对应健康分返回 null → 404 err.dq.not-found）。
     */
    RuleDetailVO findRuleDetail(String assetId, String fieldName);

    /**
     * 组装分页结果（PageQuery 自动分页的 total 在查询后回读）。
     */
    static PageResult<AssetHealthRowVO> toPage(List<AssetHealthRowVO> records, HealthScorePageQuery query) {
        return PageResult.of(records, query.getTempTotalCount(), query.getPageSize(), query.getPageIndex());
    }
}
