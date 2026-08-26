package com.yss.datamiddle.aicontextlayer.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Agent 数据域映射 PO（受控配置，数据架构 §5）。
 */
@Getter
@Setter
@TableName("agent_domain")
public class AgentDomainPO {

    /** 主键ID */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /** Agent 身份标识 */
    @TableField("agent_id")
    private String agentId;

    /** 数据域（与主平台 domain 枚举对齐，IC-01） */
    @TableField("domain")
    private String domain;

    /** 更新时间 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
