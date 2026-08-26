package com.yss.metadata.domain.collector.exception;

import com.yss.cloud.exception.BizException;

/**
 * 采集实例状态冲突（409 语义）。
 */
public class CollectorInstanceStateConflictException extends BizException {

    private static final long serialVersionUID = 1L;

    public CollectorInstanceStateConflictException(String message) {
        super("collector_instance.state_conflict", message);
    }
}
