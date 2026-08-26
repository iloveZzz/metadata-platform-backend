package com.yss.metadata.domain.collector.exception;

import com.yss.cloud.exception.BizException;

/**
 * 采集任务创建冲突（409 语义，同数据源 + 调度唯一）。
 */
public class CollectorTaskConflictException extends BizException {

    private static final long serialVersionUID = 1L;

    public CollectorTaskConflictException(String connectorId, String schedule) {
        super("collector.conflict", "同数据源与调度已存在采集任务：connectorId=" + connectorId + ", schedule=" + schedule);
    }
}
