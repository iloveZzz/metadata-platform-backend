package com.yss.datamiddle.aicontextlayer.domain.mcpserver;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计留痕（调用即写，不可变 SEC-06，数据架构 §5 / §6.1）。
 *
 * <p>初版字段；哈希链（prev_hash / row_hash）由 ACL-SLICE-05 补全（合同 seam_deferred）。
 * 本对象不承载任何凭据明文（params_summary 不含凭据，SEC-05/11）。</p>
 */
@Getter
@Setter
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private String id;

    /** 每次调用唯一 ID（关联溯源；本表唯一） */
    private String mcpRequestId;

    /** MCP 会话 ID */
    private String sessionId;

    /** 凭据主体标识 */
    private String agentId;

    /** 工具名（鉴权失败/方法拒绝记为连接级/方法级标记） */
    private String tool;

    /** 参数摘要（不含凭据；不含完整敏感参数值） */
    private String paramsSummary;

    /** 结果摘要（返回条数/耗时/结果码） */
    private String resultSummary;

    /** 结果码：success 或 MCP 错误码 */
    private String resultCode;

    /** 服务器时间（RFC3339 UTC） */
    private LocalDateTime timestamp;

    /** 耗时（毫秒） */
    private Integer durationMs;

    /** 内部权限判定标记（403/404/越权/域外剔除；仅内部可见，SEC-03） */
    private String internalPermissionFlag;
}
