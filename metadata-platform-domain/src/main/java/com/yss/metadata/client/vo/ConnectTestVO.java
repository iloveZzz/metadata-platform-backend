package com.yss.metadata.client.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 连接测试结果视图对象（冻结 OpenAPI POST /api/connectors/{id}/test 成功响应 data）。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConnectTestVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否连接成功 */
    private boolean connected;

    /** 结果文案 */
    private String message;

    public static ConnectTestVO of(boolean connected, String message) {
        return new ConnectTestVO(connected, message);
    }
}
