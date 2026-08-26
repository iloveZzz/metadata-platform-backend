package com.yss.datasecurity.repository.entity;

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

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sec_key_task_reference")
public class KeyTaskReferencePO implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("key_id")
    private Long keyId;

    @TableField("task_name")
    private String taskName;

    @TableField("sector_name")
    private String sectorName;

    @TableField("project_name")
    private String projectName;

    @TableField("task_type")
    private String taskType;

    @TableField("operation_type")
    private String operationType;

    @TableField("owner")
    private String owner;

    @TableField("last_executed_at")
    private LocalDateTime lastExecutedAt;
}
