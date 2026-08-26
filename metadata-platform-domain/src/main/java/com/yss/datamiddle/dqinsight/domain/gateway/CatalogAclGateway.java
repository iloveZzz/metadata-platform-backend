package com.yss.datamiddle.dqinsight.domain.gateway;

import com.yss.datamiddle.dqinsight.domain.model.AssetLookupResult;

/**
 * 资产对齐防腐层（CatalogAclGateway）：只读消费主平台冻结资产 API（GET /api/assets/{id}，C17）。
 *
 * <p>资产 ID 未命中 → 结果仍入库并挂待关联队列（SB-05）；网络超时 → network 分类 422。</p>
 */
public interface CatalogAclGateway {

    /**
     * 校验资产存在性并取快照字段。
     *
     * @param assetId 主平台资产 ID
     * @return FOUND（含快照）/ NOT_FOUND / NETWORK_FAILURE
     */
    AssetLookupResult lookupAsset(String assetId);

    /**
     * 数据域内可见目标资产数（覆盖率分母，SB-07 口径）。
     *
     * <p>只读消费主平台冻结资产 API（GET /api/assets，C17）；主平台按调用身份（传播用户 / 服务身份）
     * 应用自身 RBAC 可见性；domain 非空时按数据域参数收敛口径。防腐层不可用 / 解析失败返回 0
     * （targetAssetCount=0 → 覆盖率按 0 表达，切片 03 人工审查点）；缓存为数据架构 §9 既定策略（P1 / 切片 05）。</p>
     *
     * @param domain 数据域筛选（可空 = 主平台身份可见全集）
     * @return 可见目标资产数；未知时 0
     */
    int countVisibleTargetAssets(String domain);
}
