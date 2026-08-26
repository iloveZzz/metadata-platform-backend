package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计日志视图对象（冻结 OpenAPI GET /api/audit-logs 响应 data 项；只读不可变）。
 */
@Getter
@Setter
public class AuditLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 审计 id */
    private String id;

    /** 操作者 */
    private String operator;

    /** 动作 */
    private String action;

    /** 操作对象 */
    private String object;

    /** 结果 */
    private String result;

    /** 操作时间 */
    private LocalDateTime time;
}
