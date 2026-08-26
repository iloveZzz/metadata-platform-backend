package com.yss.metadata.application.lineage.service;

import com.yss.metadata.client.vo.ExportTaskVO;
import com.yss.metadata.client.vo.ImpactVO;

/**
 * 影响分析应用服务（WU-03-03 下游召回 / WU-03-04 导出任务）。
 *
 * <p>下游全量召回按深度分组（sortBy depth/domain/risk，默认 depth；0 影响空结构）；
 * 导出为 202 异步幂等任务（同资产同格式进行中复用）+ CSV/JSON 生成
 * （本地可配置目录 seam）+ audit_log 审计（impact.export）。</p>
 */
public interface ImpactAnalysisService {

    /**
     * 影响分析（下游全量召回 + 深度分组）。
     *
     * @param assetId 中心资产 id
     * @param sortBy  排序键（depth/domain/risk，默认 depth；非法抛 422）
     */
    ImpactVO getImpact(String assetId, String sortBy);

    /**
     * 导出影响分析（202 语义；幂等复用；异步任务状态流转 pending→running→success/failed）。
     *
     * @param assetId  中心资产 id
     * @param format   csv/json（非法抛 422）
     * @param operator 当前用户（X-User-Id 解析值，缺省 default-user）
     */
    ExportTaskVO exportImpact(String assetId, String format, String operator);
}
