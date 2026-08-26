package com.yss.metadata.domain.connector.exception;

import com.yss.cloud.exception.BizException;

/**
 * 连接器被引用无法删除（409 语义，冻结 OpenAPI DELETE /api/connectors/{id} 409）。
 *
 * <p>存在采集任务引用该连接器时删除返回 409，避免产生孤儿采集任务。</p>
 */
public class ConnectorReferencedException extends BizException {

    private static final long serialVersionUID = 1L;

    public ConnectorReferencedException(String id) {
        super("connector.in_use", "连接器仍被采集任务引用，无法删除：" + id);
    }
}
