package com.yss.metadata.domain.asset.exception;

import com.yss.cloud.exception.BizException;

/**
 * 资产状态冲突（409 语义）。
 *
 * <p>触发场景：重复归档、归档资产只读（编辑标签/认领被禁用）、
 * 已删除资产执行操作（认领/归档/取消归档/收藏/标签）。</p>
 */
public class AssetStateConflictException extends BizException {

    private static final long serialVersionUID = 1L;

    public AssetStateConflictException(String message) {
        super("asset.state_conflict", message);
    }
}
