package com.yss.datamiddle.semantic.term.exception;

/**
 * 术语不存在（HTTP 404）。
 */
public class TermNotFoundException extends RuntimeException {

    public TermNotFoundException(Object id) {
        super("术语不存在: " + id);
    }
}
