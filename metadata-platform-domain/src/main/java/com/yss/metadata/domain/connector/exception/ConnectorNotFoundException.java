package com.yss.metadata.domain.connector.exception;

import com.yss.cloud.exception.BizException;

/**
 * 连接器不存在（404 语义）。
 */
public class ConnectorNotFoundException extends BizException {

    private static final long serialVersionUID = 1L;

    public ConnectorNotFoundException(String id) {
        super("connector.not_found", "连接器不存在：" + id);
    }
}
