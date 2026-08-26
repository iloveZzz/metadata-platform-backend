package com.yss.metadata.domain.governance.exception;

import com.yss.cloud.exception.BizException;

/**
 * 分级分类结果不存在（404 语义）。
 */
public class ClassificationNotFoundException extends BizException {

    private static final long serialVersionUID = 1L;

    public ClassificationNotFoundException(String id) {
        super("classification.not_found", "分级分类结果不存在：" + id);
    }
}
