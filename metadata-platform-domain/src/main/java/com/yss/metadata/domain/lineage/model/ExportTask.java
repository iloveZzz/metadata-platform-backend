package com.yss.metadata.domain.lineage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 导出异步任务（数据架构 ExportTask：影响分析 CSV/JSON 导出）。
 *
 * <p>幂等：同 asset+format 进行中任务（pending/running）复用（服务层实现）；
 * 状态流转 pending→running→success/failed；触发写 audit_log 审计。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（UUID） */
    private String id;

    /** 影响分析源资产 id */
    private String assetId;

    /** 导出格式（csv/json） */
    private String format;

    /** 状态（pending/running/success/failed） */
    private ExportTaskStatus status;

    /** 生成文件引用（本地目录/对象存储） */
    private String fileRef;

    /** 触发人（X-User-Id 解析值，缺省 default-user） */
    private String operator;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 完成时间 */
    private LocalDateTime finishedAt;
}
