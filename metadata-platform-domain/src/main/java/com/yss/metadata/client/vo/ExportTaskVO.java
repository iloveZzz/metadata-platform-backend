package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 导出异步任务视图对象（冻结 OpenAPI GET /api/assets/{id}/impact-analysis/export
 * 202 响应 data；幂等复用与状态流转依据）。
 */
@Getter
@Setter
public class ExportTaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务 id */
    private String id;

    /** 影响分析源资产 id */
    private String assetId;

    /** 导出格式（csv/json） */
    private String format;

    /** 状态（pending/running/success/failed） */
    private String status;

    /** 生成文件引用 */
    private String fileRef;

    /** 触发人 */
    private String operator;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 完成时间 */
    private LocalDateTime finishedAt;
}
