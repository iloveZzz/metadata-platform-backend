package com.yss.metadata.application.dq;

import com.yss.metadata.domain.dq.gateway.RootCauseAnalysisGateway;
import com.yss.metadata.domain.dq.model.RootCauseReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 质量根因溯源应用服务
 *
 * @author ai
 * @since 2026-08-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DqRootCauseApplicationService {

    private final RootCauseAnalysisGateway rootCauseAnalysisGateway;

    /**
     * 执行质量根因溯源分析
     *
     * @param targetAssetId 目标故障资产ID
     * @return 根因溯源报告
     */
    public RootCauseReport analyzeRootCause(String targetAssetId) {
        log.info("开始对资产 [{}] 进行质量根因溯源分析", targetAssetId);
        return rootCauseAnalysisGateway.analyzeRootCause(targetAssetId);
    }
}
