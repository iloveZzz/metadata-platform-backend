package com.yss.datamiddle.aicontextlayer.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 审计留痕 PO（调用即写，不可变 SEC-06，数据架构 §5）。
 *
 * <p>初版字段；哈希链（prev_hash / row_hash）由 ACL-SLICE-05 补全。不可变：无修改 / 删除路径
 * （数据库账号最小权限）。params_summary / result_summary 不含凭据明文（SEC-05/11）。</p>
 */
@Getter
@Setter
@TableName("audit_log")
public class AuditLogPO {

    /** 主键ID */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /** 每次调用唯一 ID（关联溯源；本表唯一） */
    @TableField("mcp_request_id")
    private String mcpRequestId;

    /** MCP 会话 ID */
    @TableField("session_id")
    private String sessionId;

    /** 凭据主体标识 */
    @TableField("agent_id")
    private String agentId;

    /** 工具名（鉴权失败/方法拒绝记为连接级/方法级标记） */
    @TableField("tool")
    private String tool;

    /** 参数摘要（不含凭据；不含完整敏感参数值） */
    @TableField("params_summary")
    private String paramsSummary;

    /** 结果摘要（返回条数/耗时/结果码） */
    @TableField("result_summary")
    private String resultSummary;

    /** 结果码：success 或 MCP 错误码 */
    @TableField("result_code")
    private String resultCode;

    /** 服务器时间（RFC3339 UTC） */
    @TableField("timestamp")
    private LocalDateTime timestamp;

    /** 耗时（毫秒） */
    @TableField("duration_ms")
    private Integer durationMs;

    /** 内部权限判定标记（403/404/越权/域外剔除；仅内部可见，SEC-03） */
    @TableField("internal_permission_flag")
    private String internalPermissionFlag;
}
