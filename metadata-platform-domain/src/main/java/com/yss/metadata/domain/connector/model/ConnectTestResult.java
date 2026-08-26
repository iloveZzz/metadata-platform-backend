package com.yss.metadata.domain.connector.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 连接测试结果值对象。
 *
 * <p>连接成功时 connected=true、errorType 为空；失败时携带错误分类
 * （network/credential/dialect）与可读文案。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectTestResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否连接成功 */
    private boolean connected;

    /** 失败时的错误分类 */
    private ConnectErrorType errorType;

    /** 结果 / 失败文案 */
    private String message;

    public static ConnectTestResult success(String message) {
        return ConnectTestResult.builder().connected(true).message(message).build();
    }

    public static ConnectTestResult failure(ConnectErrorType errorType, String message) {
        return ConnectTestResult.builder().connected(false).errorType(errorType).message(message).build();
    }
}
