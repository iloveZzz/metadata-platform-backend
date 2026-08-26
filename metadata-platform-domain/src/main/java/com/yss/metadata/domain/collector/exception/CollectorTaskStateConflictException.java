package com.yss.metadata.domain.collector.exception;

import com.yss.cloud.exception.BizException;

/**
 * 采集任务状态冲突（409 语义）。
 *
 * <p>触发场景：运行中重复触发（幂等拒绝）、取消非运行中任务、
 * 非法状态跳转（如已取消→运行中）。</p>
 */
public class CollectorTaskStateConflictException extends BizException {

    private static final long serialVersionUID = 1L;

    public CollectorTaskStateConflictException(String message) {
        super("collector.state_conflict", message);
    }
}
