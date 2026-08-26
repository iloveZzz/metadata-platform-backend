package com.yss.datamiddle.dqinsight.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 接入通道持久化对象（dq_channel；name 未删除唯一；auth_token_enc 加密存储，密文不回传）。
 */
@Getter
@Setter
@TableName("dq_channel")
public class DqChannelPO {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 通道名（UNIQUE，重名 409 name-conflict） */
    @TableField("name")
    private String name;

    /** 通道类型（api-push / scheduled-pull） */
    @TableField("type")
    private String type;

    /** 拉取周期（cron；仅 scheduled-pull） */
    @TableField("schedule")
    private String schedule;

    /** 格式类型（GE / 通用 CSV / 通用 API） */
    @TableField("format_type")
    private String formatType;

    /** 认证凭证密文（成熟加密库；密文不回传） */
    @TableField("auth_token_enc")
    private String authTokenEnc;

    /** 认证是否已配置（对外仅此布尔） */
    @TableField("auth_configured")
    private Boolean authConfigured;

    /** 目标数据域（缺省 = 全数据域） */
    @TableField("domain")
    private String domain;

    /** 通道状态（enabled / disabled / pulling / pull-failed） */
    @TableField("state")
    private String state;

    /** 上次拉取时间 */
    @TableField("last_pull_at")
    private LocalDateTime lastPullAt;

    /** 错误信息（脱敏） */
    @TableField("last_error")
    private String lastError;

    /** 错误分类（format / auth / network） */
    @TableField("error_category")
    private String errorCategory;

    /** 软删除位（MVP 无历史结果时物理删除，保留字段防御） */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;

    /** 创建人 */
    @TableField("created_by")
    private String createdBy;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间（乐观并发版本位） */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
