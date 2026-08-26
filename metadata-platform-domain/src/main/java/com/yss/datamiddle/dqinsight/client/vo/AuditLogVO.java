package com.yss.datamiddle.dqinsight.client.vo;

import com.yss.datamiddle.dqinsight.domain.model.AuditAction;
import com.yss.datamiddle.dqinsight.domain.model.AuditResult;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 审计日志条目（冻结 OpenAPI AuditLogEntry，dq_audit_log 只读不可变 append-only）。
 *
 * <p>字段对齐冻结契约：time / operator / action / object / result / detail；id 附加
 * 供前端列表 key（追加字段不改变契约语义）。detail 已脱敏（写入路径保证，C19 / C27）。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class AuditLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 审计记录 ID */
    private String id;

    /** 发生时间（ISO 8601） */
    private String time;

    /** 操作者（外部通道推送为通道标识 / system） */
    private String operator;

    /** 动作（7 类枚举） */
    private AuditAction action;

    /** 对象引用（批次号等） */
    private String object;

    /** 结果（success / failure） */
    private AuditResult result;

    /** 详情（脱敏） */
    private String detail;
}
