package com.yss.metadata.domain.lineage.gateway;

import com.yss.metadata.domain.lineage.model.ImpactNode;

import java.util.List;

/**
 * 影响分析查询端口（血缘域；Domain 定义，Infrastructure 实现）。
 *
 * <p>下游全量召回：原生 SQL 递归 CTE（收敛于基础设施端口实现，参数绑定），
 * 环保护 + 深度上限；0 影响返回空列表（非错误）。</p>
 */
public interface ImpactAnalysisRepository {

    /**
     * 查询指定资产的全部下游（含深度）；maxDepth 为递归深度上限。
     */
    List<ImpactNode> findDownstream(String assetId, int maxDepth);
}
