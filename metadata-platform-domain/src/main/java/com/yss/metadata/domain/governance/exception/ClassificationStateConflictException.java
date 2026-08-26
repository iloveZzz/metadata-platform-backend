package com.yss.metadata.domain.governance.exception;

import com.yss.cloud.exception.BizException;

/**
 * 分级分类状态冲突（409 语义；如修正分类名为空、已删除状态操作）。
 */
public class ClassificationStateConflictException extends BizException {

    private static final long serialVersionUID = 1L;

    public ClassificationStateConflictException(String message) {
        super("classification.state_conflict", message);
    }
}
