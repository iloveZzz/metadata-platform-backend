package com.yss.datamiddle.aicontextlayer.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MCP 会话 PO（数据架构 §5）。
 */
@Getter
@Setter
@TableName("mcp_session")
public class McpSessionPO {

    /** 主键ID（会话ID，数据架构 §5） */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /** Agent 身份标识 */
    @TableField("agent_id")
    private String agentId;

    /** 会话绑定的凭据版本 */
    @TableField("credential_version")
    private String credentialVersion;

    /** 会话状态：ACTIVE/EXPIRED/TERMINATED */
    @TableField("status")
    private String status;

    /** 建立时间 */
    @TableField("established_at")
    private LocalDateTime establishedAt;

    /** 最近活跃时间（空闲回收依据 REC-05） */
    @TableField("last_active_at")
    private LocalDateTime lastActiveAt;

    /** 过期时间（会话最大时长） */
    @TableField("expires_at")
    private LocalDateTime expiresAt;

    /** 终止时间（吊销强制断开/显式终止） */
    @TableField("terminated_at")
    private LocalDateTime terminatedAt;
}
