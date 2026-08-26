package com.yss.metadata.domain.connector.exception;

import com.yss.cloud.exception.BizException;

/**
 * 连接器名称冲突（409 语义，name 唯一）。
 */
public class ConnectorNameConflictException extends BizException {

    private static final long serialVersionUID = 1L;

    public ConnectorNameConflictException(String name) {
        super("connector.name_conflict", "连接器名称已存在：" + name);
    }
}
