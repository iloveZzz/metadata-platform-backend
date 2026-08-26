package com.yss.datamiddle.semantic.term.exception;

/**
 * 引用冲突（HTTP 409 REFERENCE_CONFLICT）。
 *
 * <p>典型场景：删除被挂接 / 被同义词组关联的术语——提示改用弃用（SB-09）。</p>
 */
public class ReferenceConflictException extends RuntimeException {

    public ReferenceConflictException(String message) {
        super(message);
    }
}
