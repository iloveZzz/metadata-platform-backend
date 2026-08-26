package com.yss.metadata.domain.connector.model;

/**
 * 连接测试错误分类（合同 WU-01-01：network/credential/dialect）。
 *
 * <p>code 为冻结 OpenAPI Error.code 使用的业务错误码前缀。</p>
 */
public enum ConnectErrorType {

    /** 网络不可达 / 主机端口 / 超时 */
    NETWORK("err.connector.network"),
    /** 用户名 / 密码等凭据不正确 */
    CREDENTIAL("err.connector.credential"),
    /** 方言不受支持 / 未认证（如 GaussDB PoC 未完成） */
    DIALECT("err.connector.dialect");

    private final String code;

    ConnectErrorType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
