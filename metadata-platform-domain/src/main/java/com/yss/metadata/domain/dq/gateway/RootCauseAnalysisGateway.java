package com.yss.metadata.domain.dq.gateway;

import com.yss.metadata.domain.dq.model.RootCauseReport;

/**
 * 质量-血缘联合根因溯源网关接口
 *
 * @author ai
 * @since 2026-08-15
 */
public interface RootCauseAnalysisGateway {

    /**
     * 对目标资产执行上游拓扑溯源分析
     *
     * @param targetAssetId 目标资产ID
     * @return 根因分析报告
     */
    RootCauseReport analyzeRootCause(String targetAssetId);
}
