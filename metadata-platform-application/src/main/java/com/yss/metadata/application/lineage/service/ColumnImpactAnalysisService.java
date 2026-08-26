package com.yss.metadata.application.lineage.service;

import com.yss.metadata.client.vo.ColumnImpactAnalysisVO;

/**
 * 字段级爆炸半径 (Blast Radius) 影响分析应用服务。
 */
public interface ColumnImpactAnalysisService {

    /**
     * 计算指定资产字段的下游爆炸半径与级联影响图。
     *
     * @param assetId   源资产 ID
     * @param columnId  源字段 ID 或名称
     * @param maxDepth  最大遍历深度 (默认 5)
     * @return 字段级影响分析结果 VO
     */
    ColumnImpactAnalysisVO analyzeImpact(String assetId, String columnId, Integer maxDepth);
}
