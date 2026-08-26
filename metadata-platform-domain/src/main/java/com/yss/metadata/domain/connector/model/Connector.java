package com.yss.metadata.domain.connector.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 连接器聚合根（数据架构 DataSource）。
 *
 * <p>核心规则：状态机（草稿/已连接/失败/停用）、配置变更后重置草稿需重新测试、
 * 领域不变量校验；凭据仅保存加密引用（credentialRef），密码明文不落库。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Connector implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（OpenAPI 类型为 string） */
    private String id;

    /** 连接器名称（唯一） */
    private String name;

    /** 连接器类型 */
    private ConnectorType type;

    /** 主机地址 */
    private String host;

    /** 端口（1-65535） */
    private Integer port;

    /** 方言 */
    private Dialect dialect;

    /** 用户名 */
    private String username;

    /** 凭据加密引用，替代明文密码 */
    private String credentialRef;

    /** 是否自动识别分类（默认 true） */
    private Boolean autoClassify;

    /** 当前状态 */
    private ConnectorStatus status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 最后更新时间 */
    private LocalDateTime updatedAt;

    /**
     * 测试连接成功：状态流转为已连接。
     */
    public void markConnected() {
        this.status = ConnectorStatus.CONNECTED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 测试连接失败：状态流转为失败。
     */
    public void markTestFailed() {
        this.status = ConnectorStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 停用：状态流转为停用。
     */
    public void disable() {
        this.status = ConnectorStatus.DISABLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 配置更新（PUT 全量替换语义）。
     *
     * <p>配置变更后状态重置为草稿，需重新测试连接；更新时间刷新。</p>
     */
    public void update(String name, ConnectorType type, String host, Integer port, Dialect dialect,
                       String username, String credentialRef, Boolean autoClassify) {
        this.name = name;
        this.type = type;
        this.host = host;
        this.port = port;
        this.dialect = dialect;
        this.username = username;
        this.credentialRef = credentialRef;
        this.autoClassify = autoClassify == null || autoClassify;
        this.status = ConnectorStatus.DRAFT;
        this.updatedAt = LocalDateTime.now();
        validate();
    }

    /**
     * 领域不变量校验：名称/主机/类型/方言非空，端口在 1-65535 之间。
     */
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("连接器名称不能为空");
        }
        if (type == null) {
            throw new IllegalArgumentException("连接器类型不能为空");
        }
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("主机地址不能为空");
        }
        if (port == null || port < 1 || port > 65535) {
            throw new IllegalArgumentException("端口必须在 1-65535 之间");
        }
        if (dialect == null) {
            throw new IllegalArgumentException("方言不能为空");
        }
    }
}
