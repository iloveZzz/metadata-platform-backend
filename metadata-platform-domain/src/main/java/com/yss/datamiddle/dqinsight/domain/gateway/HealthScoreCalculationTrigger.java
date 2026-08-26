package com.yss.datamiddle.dqinsight.domain.gateway;

import java.util.List;

/**
 * 健康分计算触发 seam（关联命中 → 触发健康分计算）。
 *
 * <p>计算服务由切片 02 落地；本切片以 seam 端口接驳，切片 01 内为占位实现（合同 seam_deferred）。</p>
 */
public interface HealthScoreCalculationTrigger {

    /**
     * 触发指定资产（关联命中）的健康分计算。
     *
     * @param batchId       来源批次 ID
     * @param linkedAssetIds 关联命中的资产 ID 列表
     */
    void triggerForAssets(String batchId, List<String> linkedAssetIds);
}
