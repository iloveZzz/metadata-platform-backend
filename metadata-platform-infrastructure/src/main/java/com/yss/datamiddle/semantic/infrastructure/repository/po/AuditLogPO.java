package com.yss.datamiddle.semantic.infrastructure.repository.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 审计日志持久化对象（audit_log 表，不可变只追加）。
 */
@Getter
@Setter
@TableName("audit_log")
public class AuditLogPO {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("operator")
    private String operator;

    @TableField("action")
    private String action;

    @TableField("object_type")
    private String objectType;

    @TableField("object_id")
    private Long objectId;

    @TableField("note")
    private String note;

    @TableField("result")
    private String result;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
