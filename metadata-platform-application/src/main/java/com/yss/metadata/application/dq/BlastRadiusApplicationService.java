package com.yss.metadata.application.dq;

import com.yss.metadata.domain.dq.gateway.BlastRadiusGateway;
import com.yss.metadata.domain.dq.model.BlastRadiusReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 下游爆炸半径分析应用服务
 *
 * @author ai
 * @since 2026-08-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlastRadiusApplicationService {

    private final BlastRadiusGateway blastRadiusGateway;

    /**
     * 计算指定资产在下游血缘中的爆炸半径
     *
     * @param originAssetId 源资产ID
     * @param maxDepth      最大深度
     * @return 爆炸半径报告
     */
    public BlastRadiusReport calculateBlastRadius(String originAssetId, int maxDepth) {
        log.info("开始计算资产 [{}] 下游爆炸半径 (maxDepth={})", originAssetId, maxDepth);
        return blastRadiusGateway.calculateBlastRadius(originAssetId, maxDepth);
    }
}
