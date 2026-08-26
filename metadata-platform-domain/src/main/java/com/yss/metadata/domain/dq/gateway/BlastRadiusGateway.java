package com.yss.metadata.domain.dq.gateway;

import com.yss.metadata.domain.dq.model.BlastRadiusReport;

/**
 * 下游爆炸半径计算网关接口
 *
 * @author ai
 * @since 2026-08-15
 */
public interface BlastRadiusGateway {

    /**
     * 计算指定资产在下游血缘中的爆炸半径
     *
     * @param originAssetId 源资产ID
     * @param maxDepth      最大向下遍历深度
     * @return 爆炸半径报告
     */
    BlastRadiusReport calculateBlastRadius(String originAssetId, int maxDepth);
}
