package com.yss.datamiddle.aicontextlayer.domain.mcpserver;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 只读工具白名单（SEC-09，数据架构 §5）。
 *
 * <p>白名单恰好 5 个只读工具（冻结契约第 2 节），无写 / 执行 / 管理工具；
 * 种子数据冻结，变更走冻结后变更流程。本 WU 只落表与 seed，不做注册行为（WU05 承接）。</p>
 */
@Getter
@Setter
public class ToolRegistry implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工具名（白名单主键） */
    private String toolName;

    /** 工具版本（契约 v1.0-frozen 对应 1.0.0） */
    private String version;

    /** 启用状态：1-启用，0-停用 */
    private Integer enabled;

    /** 参数 schema 引用（契约工具定义章节） */
    private String paramsSchemaRef;
}
