package com.yss.metadata.domain.collector.gateway;

import com.yss.metadata.domain.collector.model.CollectedAsset;
import com.yss.metadata.domain.collector.model.SavedAssetRef;

import java.util.List;

/**
 * 资产入库网关端口（Domain 定义，Infrastructure 实现）。
 *
 * <p>采集上下文产出资产清单后经本端口幂等入库：同数据源 + 名称的资产 upsert
 * （列全量替换），并生成资产版本快照（version 递增，schema_diff 记录列快照）。</p>
 */
public interface AssetGateway {

    /**
     * 将采集到的资产幂等入库并生成版本快照。
     *
     * @param sourceId 来源数据源（连接器）id，对应 asset.source_id
     * @param assets   采集产物资产清单
     * @return 已入库资产引用（资产 id + 列 id；切片 04 新增，供 autoClassify 识别挂载）
     */
    List<SavedAssetRef> saveAssets(String sourceId, List<CollectedAsset> assets);
}
