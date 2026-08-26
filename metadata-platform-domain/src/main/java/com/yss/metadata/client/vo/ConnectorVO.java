package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 连接器视图对象（冻结 OpenAPI Connector 响应 data）。
 *
 * <p>不暴露密码/凭据引用等敏感字段。</p>
 */
@Getter
@Setter
public class ConnectorVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 连接器 id */
    private String id;

    /** 连接器名称 */
    private String name;

    /** 连接器类型（如 "MySQL" / "OSS/S3"） */
    private String type;

    /** 主机地址 */
    private String host;

    /** 端口 */
    private Integer port;

    /** 方言（如 "native" / "mysql-compatible"） */
    private String dialect;

    /** 用户名 */
    private String username;

    /** 是否自动识别分类 */
    private Boolean autoClassify;

    /** 状态（draft/connected/failed/disabled） */
    private String status;
}
