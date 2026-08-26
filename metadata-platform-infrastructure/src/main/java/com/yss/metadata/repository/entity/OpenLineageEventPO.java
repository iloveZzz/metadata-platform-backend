package com.yss.metadata.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * OpenLineage 事件接收记录持久化对象（openlineage_event 表；WU-05-02）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("openlineage_event")
public class OpenLineageEventPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("event_type")
    private String eventType;

    @TableField("event_time")
    private LocalDateTime eventTime;

    @TableField("run_id")
    private String runId;

    @TableField("job_namespace")
    private String jobNamespace;

    @TableField("job_name")
    private String jobName;

    @TableField("parse_status")
    private String parseStatus;

    @TableField("received_at")
    private LocalDateTime receivedAt;
}
