package com.yss.datamiddle.dqinsight.domain.exception;

import com.yss.datamiddle.dqinsight.domain.constant.DqErrorCodes;

/**
 * 目标资产不存在（422 err.dq.asset.not-found：防腐层消费冻结 GET /api/assets 校验，C26）。
 */
public class AssetNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AssetNotFoundException(String assetId) {
        super("目标资产不存在（主平台校验）：assetId=" + assetId);
    }

    public String getErrCode() {
        return DqErrorCodes.ASSET_NOT_FOUND;
    }
}
