package com.yss.metadata.domain.connector.exception;

import com.yss.cloud.exception.BizException;
import com.yss.metadata.domain.connector.model.ConnectErrorType;

/**
 * 连接测试失败（422 语义），携带错误分类（network/credential/dialect）。
 *
 * <p>作为业务期望的失败结果抛出（不视为系统异常）；
 * 失败时连接器状态已持久化为失败，事务通过 noRollbackFor 保留该状态。</p>
 */
public class ConnectTestException extends BizException {

    private static final long serialVersionUID = 1L;

    private final ConnectErrorType errorType;

    public ConnectTestException(ConnectErrorType errorType, String message) {
        super(errorType.getCode(), message);
        this.errorType = errorType;
    }

    public ConnectErrorType getErrorType() {
        return errorType;
    }
}
