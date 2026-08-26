package com.yss.datamiddle.semantic.term.exception;

/**
 * 状态冲突（HTTP 409 STATE_CONFLICT）。
 *
 * <p>典型场景：删除非草稿术语、认证已弃用术语。</p>
 */
public class StateConflictException extends RuntimeException {

    public StateConflictException(String message) {
        super(message);
    }
}
