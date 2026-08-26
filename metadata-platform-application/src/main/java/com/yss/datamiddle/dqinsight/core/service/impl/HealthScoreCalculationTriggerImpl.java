package com.yss.datamiddle.dqinsight.core.service.impl;

import com.yss.datamiddle.dqinsight.core.service.HealthScoreCalculationAppService;
import com.yss.datamiddle.dqinsight.domain.gateway.HealthScoreCalculationTrigger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 健康分计算触发实现（切片 01 seam 接驳闭环，合同 seam_deferred verification_plan）。
 *
 * <p>切片 01 关联命中后调用本触发（原 LoggingHealthScoreCalculationTrigger 占位由本实现替换）：
 * 接入 → 计算 → 档位端到端闭环；切片 04 人工映射复用同一服务入口（合同 02/04 seam）。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HealthScoreCalculationTriggerImpl implements HealthScoreCalculationTrigger {

    private final HealthScoreCalculationAppService healthScoreCalculationAppService;

    @Override
    public void triggerForAssets(String batchId, List<String> linkedAssetIds) {
        healthScoreCalculationAppService.calculateForAssets(batchId, linkedAssetIds);
    }
}
