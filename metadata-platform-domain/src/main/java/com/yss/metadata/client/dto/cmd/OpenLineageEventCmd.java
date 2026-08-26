package com.yss.metadata.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;
import com.yss.metadata.domain.integration.model.OpenLineageEventType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OpenLineage 事件接收命令（POST /api/v1/lineage；冻结 API OpenLineageEvent）。
 *
 * <p>外部标准协议入口：eventType/eventTime/run.runId/job.namespace+name/
 * inputs/outputs（namespace+name）。校验失败由 Web 层统一映射 422。</p>
 */
@Getter
@Setter
public class OpenLineageEventCmd extends CommandDTO {

    private static final long serialVersionUID = 1L;

    /** 事件类型（START/COMPLETE/FAIL/ABORT） */
    private OpenLineageEventType eventType;

    /** 事件时间 */
    private LocalDateTime eventTime;

    /** Run 标识（run.runId） */
    private OpenLineageRunCmd run;

    /** 作业（job.namespace + job.name） */
    private OpenLineageJobCmd job;

    /** 输入数据集 */
    private List<OpenLineageDatasetCmd> inputs;

    /** 输出数据集 */
    private List<OpenLineageDatasetCmd> outputs;
}
