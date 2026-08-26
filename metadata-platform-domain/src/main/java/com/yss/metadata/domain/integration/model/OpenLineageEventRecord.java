package com.yss.metadata.domain.integration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * OpenLineage 事件接收记录（数据架构 OpenLineageEvent：事件统计近 24h/解析成功率依据）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenLineageEventRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件记录主键（UUID） */
    private String id;

    /** 事件类型（START/COMPLETE/FAIL/ABORT） */
    private String eventType;

    /** 事件时间 */
    private LocalDateTime eventTime;

    /** Run 标识 */
    private String runId;

    /** 作业命名空间 */
    private String jobNamespace;

    /** 作业名 */
    private String jobName;

    /** 解析状态（received/parsed/parse_failed） */
    private OpenLineageParseStatus parseStatus;

    /** 接收时间 */
    private LocalDateTime receivedAt;
}
