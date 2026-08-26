package com.yss.datamiddle.semantic.term.exception;

import com.yss.datamiddle.semantic.term.model.Term;

/**
 * 乐观锁版本冲突（HTTP 409，冻结契约 VersionConflict 响应）。
 *
 * <p>拒绝覆盖他人修改；携带最新对象（CT-04：409 VERSION_CONFLICT + 最新对象）。</p>
 */
public class VersionConflictException extends RuntimeException {

    private final Term latest;

    public VersionConflictException(String message, Term latest) {
        super(message);
        this.latest = latest;
    }

    public Term getLatest() {
        return latest;
    }
}
