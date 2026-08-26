package com.yss.metadata.domain.connector.model;

/**
 * 连接器状态（数据架构：草稿/已连接/失败/停用）。
 */
public enum ConnectorStatus {

    /** 草稿：已创建但尚未测试连接成功 */
    DRAFT("draft"),
    /** 已连接：最近一次测试连接成功 */
    CONNECTED("connected"),
    /** 失败：最近一次测试连接失败 */
    FAILED("failed"),
    /** 停用 */
    DISABLED("disabled");

    private final String value;

    ConnectorStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
