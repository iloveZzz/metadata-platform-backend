package com.yss.metadata.domain.governance.exception;

import com.yss.cloud.exception.BizException;

/**
 * 分类规则不存在（404 语义）。
 */
public class ClassRuleNotFoundException extends BizException {

    private static final long serialVersionUID = 1L;

    public ClassRuleNotFoundException(String id) {
        super("class_rule.not_found", "分类规则不存在：" + id);
    }
}
