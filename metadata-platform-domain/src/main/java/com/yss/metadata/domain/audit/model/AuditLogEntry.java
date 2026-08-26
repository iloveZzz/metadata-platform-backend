package com.yss.metadata.domain.audit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计日志条目（数据架构 AuditLog：操作者/动作/对象/时间；不可变）。
 *
 * <p>本切片基础写入（人工补录 lineage.manual / 影响分析导出 impact.export），
 * 审计完备化（RBAC/审计页面）属 slice 06。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 审计 id（UUID） */
    private String id;

    /** 操作者（X-User-Id 解析值，缺省 default-user） */
    private String operator;

    /** 动作（如 lineage.manual / impact.export） */
    private String action;

    /** 操作对象（如资产 id / 任务 id） */
    private String object;

    /** 结果（success/failed） */
    private String result;

    /** 操作时间 */
    private LocalDateTime time;
}
