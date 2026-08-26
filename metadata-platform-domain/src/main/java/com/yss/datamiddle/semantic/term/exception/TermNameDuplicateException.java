package com.yss.datamiddle.semantic.term.exception;

/**
 * 术语名称重复（422 字段级错误，fieldErrors.code = TERM_NAME_DUPLICATE）。
 */
public class TermNameDuplicateException extends BusinessValidationException {

    private static final String ERROR_CODE = "TERM_NAME_DUPLICATE";

    public TermNameDuplicateException(String name) {
        super("PARAM_VALIDATION_ERROR", "name", ERROR_CODE, "术语名称已存在: " + name);
    }
}
