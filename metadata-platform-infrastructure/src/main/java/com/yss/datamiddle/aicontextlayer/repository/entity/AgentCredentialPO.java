package com.yss.datamiddle.aicontextlayer.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Agent 凭据 PO（KMS 密文引用，数据架构 §5）。
 *
 * <p>安全（SEC-05）：{@code credentialRef} 仅承载 KMS 密文引用（非本地明文），
 * 本对象 / 日志 / 配置均不得出现凭据明文。本 WU 仅建字段，不接 KMS（WU03 / D3 承接）。</p>
 */
@Getter
@Setter
@TableName("agent_credential")
public class AgentCredentialPO {

    /** 主键ID */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /** Agent 身份标识 */
    @TableField("agent_id")
    private String agentId;

    /** 凭据版本（吊销/轮换按版本标记） */
    @TableField("credential_version")
    private String credentialVersion;

    /** KMS 密文引用（不存明文，SEC-05/D3） */
    @TableField("credential_ref")
    private String credentialRef;

    /** 凭据状态：ACTIVE/REVOKED/ROTATED/EXPIRED */
    @TableField("status")
    private String status;

    /** 签发时间 */
    @TableField("issued_at")
    private LocalDateTime issuedAt;

    /** 过期时间 */
    @TableField("expires_at")
    private LocalDateTime expiresAt;

    /** 吊销时间 */
    @TableField("revoked_at")
    private LocalDateTime revokedAt;
}
