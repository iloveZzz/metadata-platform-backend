package com.yss.metadata.domain.collector.exception;

import com.yss.cloud.exception.BizException;

/**
 * 采集任务不存在（404 语义）。
 */
public class CollectorTaskNotFoundException extends BizException {

    private static final long serialVersionUID = 1L;

    public CollectorTaskNotFoundException(String id) {
        super("collector.not_found", "采集任务不存在：" + id);
    }
}
