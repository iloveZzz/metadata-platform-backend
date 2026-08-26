package com.yss.datamiddle.dqinsight.core.service;

import java.util.List;

/**
 * 健康分计算用例（触发：关联命中 / 人工映射，同一服务入口；DQI-SLICE-02 application_boundary）。
 *
 * <p>Application 编排触发与事务（计算幂等可重算）；加权 / 档位领域规则在 Domain（HealthScoreEngine
 * 内部规则引擎，进程内同步执行，C10）。</p>
 */
public interface HealthScoreCalculationAppService {

    /**
     * 对指定资产（关联命中）执行健康分计算。
     *
     * @param batchId  来源批次 ID
     * @param assetIds 关联命中的资产 ID 列表
     */
    void calculateForAssets(String batchId, List<String> assetIds);
}
