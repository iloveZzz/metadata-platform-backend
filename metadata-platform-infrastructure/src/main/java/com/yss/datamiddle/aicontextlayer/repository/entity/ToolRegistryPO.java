package com.yss.datamiddle.aicontextlayer.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 只读工具白名单 PO（SEC-09，数据架构 §5）。
 *
 * <p>tool_name 为自然主键（白名单），由冻结 seed 写入，主键策略为 INPUT（不自动分配）。
 * 白名单恰好 5 个只读工具（契约第 2 节）；本 WU 只落表 + seed，不做注册行为（WU05 承接）。</p>
 */
@Getter
@Setter
@TableName("tool_registry")
public class ToolRegistryPO {

    /** 工具名（白名单主键） */
    @TableId(value = "tool_name", type = IdType.INPUT)
    private String toolName;

    /** 工具版本（契约 v1.0-frozen 对应 1.0.0） */
    @TableField("version")
    private String version;

    /** 启用状态：1-启用，0-停用 */
    @TableField("enabled")
    private Integer enabled;

    /** 参数 schema 引用（契约工具定义章节） */
    @TableField("params_schema_ref")
    private String paramsSchemaRef;
}
