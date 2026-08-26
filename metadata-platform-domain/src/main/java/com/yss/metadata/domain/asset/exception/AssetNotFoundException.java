package com.yss.metadata.domain.asset.exception;

import com.yss.cloud.exception.BizException;

/**
 * 资产不存在（404 语义）。
 */
public class AssetNotFoundException extends BizException {

    private static final long serialVersionUID = 1L;

    public AssetNotFoundException(String id) {
        super("asset.not_found", "资产不存在：" + id);
    }
}
