package com.yss.datamiddle.dqinsight.domain.model;

import lombok.Getter;

import java.io.Serializable;

/**
 * 资产校验结果（防腐层 CatalogAclGateway 端口语义）。
 *
 * <p>FOUND=命中（含快照）；NOT_FOUND=未命中（挂待关联队列 pending，不阻断入库）；
 * NETWORK_FAILURE=网络超时 / 连接失败（422 err.dq.network.timeout，network 分类）。</p>
 */
@Getter
public class AssetLookupResult implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum LookupType {
        FOUND,
        NOT_FOUND,
        NETWORK_FAILURE
    }

    private final LookupType type;

    private final String assetId;

    private final AssetSnapshot snapshot;

    private AssetLookupResult(LookupType type, String assetId, AssetSnapshot snapshot) {
        this.type = type;
        this.assetId = assetId;
        this.snapshot = snapshot;
    }

    public static AssetLookupResult found(String assetId, AssetSnapshot snapshot) {
        return new AssetLookupResult(LookupType.FOUND, assetId, snapshot);
    }

    public static AssetLookupResult notFound(String assetId) {
        return new AssetLookupResult(LookupType.NOT_FOUND, assetId, null);
    }

    public static AssetLookupResult networkFailure(String assetId) {
        return new AssetLookupResult(LookupType.NETWORK_FAILURE, assetId, null);
    }
}
