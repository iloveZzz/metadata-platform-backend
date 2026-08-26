package com.yss.metadata.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计日志持久化对象（audit_log 表；不可变，仅追加写入）。
 *
 * <p>本切片基础写入（lineage.manual / impact.export）；审计查询与完备化属 slice 06。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("audit_log")
public class AuditLogPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("operator")
    private String operator;

    @TableField("action")
    private String action;

    @TableField("object")
    private String object;

    @TableField("result")
    private String result;

    @TableField("time")
    private LocalDateTime time;
}
