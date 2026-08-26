package com.yss.datamiddle.dqinsight.core.service;

import com.yss.datamiddle.dqinsight.client.vo.IngestionReceiptVO;

/**
 * 结果接入应用服务（Application 只编排，不承载解析与加权领域规则；事务边界在用例层）。
 *
 * <p>用例编排：通道 Token 认证校验（中间件）→ 接入适配层解析与错误分类 → 幂等去重 →
 * 批次 + 规则明细入库 → 资产关联解析（CatalogAclGateway 防腐层）→ 关联命中发布健康分计算触发
 * （切片 02 seam）→ 审计写 ingest / parse-fail。</p>
 */
public interface IngestionAppService {

    /**
     * 外部 DQ 结果接入（POST /api/dq/results）。
     *
     * @param rawBody       原始内容（JSON / CSV 文本）
     * @param contentType   Content-Type（application/json / text/csv）
     * @param authChannelId 通道认证绑定的通道 ID（中间件写入；可为 null）
     * @return 接入接收回执（201，含批次状态与关联状态）
     */
    IngestionReceiptVO ingest(String rawBody, String contentType, String authChannelId);
}
