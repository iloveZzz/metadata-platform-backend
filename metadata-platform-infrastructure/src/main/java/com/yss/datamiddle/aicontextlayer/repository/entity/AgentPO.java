package com.yss.datamiddle.aicontextlayer.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Agent 身份主数据 PO（受控配置，数据架构 §5）。
 */
@Getter
@Setter
@TableName("agent")
public class AgentPO {

    /** 主键ID（受控配置） */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /** Agent 名称 */
    @TableField("name")
    private String name;

    /** 启用状态：1-启用，0-停用 */
    @TableField("enabled")
    private Integer enabled;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
