package com.yss.datamiddle.aicontextlayer.domain.mcpserver;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Agent 身份主数据（受控配置，数据架构 §5）。
 *
 * <p>仅承载身份主数据；凭据密文不在此模型（见 {@link AgentCredential#getCredentialRef()}，
 * KMS 密文引用，SEC-05）。</p>
 */
@Getter
@Setter
public class Agent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID（受控配置） */
    private String id;

    /** Agent 名称 */
    private String name;

    /** 启用状态：1-启用，0-停用 */
    private Integer enabled;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
