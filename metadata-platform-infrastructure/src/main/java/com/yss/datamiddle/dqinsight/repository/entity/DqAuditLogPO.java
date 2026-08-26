package com.yss.datamiddle.dqinsight.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 审计记录持久化对象（dq_audit_log；只读不可变 append-only，仅 INSERT）。
 */
@Getter
@Setter
@TableName("dq_audit_log")
public class DqAuditLogPO {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 发生时间 */
    @TableField("event_time")
    private LocalDateTime eventTime;

    /** 操作者 */
    @TableField("operator")
    private String operator;

    /** 动作（7 类枚举） */
    @TableField("action")
    private String action;

    /** 对象引用（批次号等） */
    @TableField("object")
    private String object;

    /** 结果（success / failure） */
    @TableField("result")
    private String result;

    /** 详情（脱敏） */
    @TableField("detail")
    private String detail;
}
