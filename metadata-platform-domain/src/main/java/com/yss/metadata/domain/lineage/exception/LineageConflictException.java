package com.yss.metadata.domain.lineage.exception;

import com.yss.cloud.exception.BizException;

/**
 * 图版本冲突（409 语义，CONFLICT）。
 *
 * <p>人工补录携带的 graphVersionToken 与当前图版本不匹配（他人在同一图并发
 * 补录后版本已变化）；恢复路径=客户端重读血缘图谱获取最新 token 后重试。
 * 错误码 {@code lineage.conflict}。</p>
 */
public class LineageConflictException extends BizException {

    private static final long serialVersionUID = 1L;

    public LineageConflictException(String message) {
        super("lineage.conflict", message);
    }
}
