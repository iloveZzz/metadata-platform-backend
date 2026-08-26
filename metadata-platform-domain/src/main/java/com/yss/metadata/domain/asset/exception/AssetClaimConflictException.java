package com.yss.metadata.domain.asset.exception;

import com.yss.cloud.exception.BizException;

/**
 * 资产认领冲突（409 语义）。
 *
 * <p>触发场景：资产已被他人认领（owner 唯一），当前用户无权认领。</p>
 */
public class AssetClaimConflictException extends BizException {

    private static final long serialVersionUID = 1L;

    public AssetClaimConflictException(String message) {
        super("asset.claim_conflict", message);
    }
}
