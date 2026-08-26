package com.yss.metadata.domain.collector.exception;

import com.yss.cloud.exception.BizException;

/**
 * 采集实例不存在（404 语义）。
 */
public class CollectorInstanceNotFoundException extends BizException {

    private static final long serialVersionUID = 1L;

    public CollectorInstanceNotFoundException(String id) {
        super("collector_instance.not_found", "采集实例不存在：" + id);
    }
}
