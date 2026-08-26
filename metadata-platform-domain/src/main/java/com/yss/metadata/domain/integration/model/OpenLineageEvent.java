package com.yss.metadata.domain.integration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenLineage 事件（冻结 API OpenLineageEvent：eventType/eventTime/run/job/inputs/outputs）。
 *
 * <p>事件接收后：数据集映射为资产（source_id=namespace、name=dataset name）与血缘边
 * （inputs → outputs，type=job，confidence=auto-high）；COMPLETE 事件产出血缘边。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenLineageEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件类型（START/COMPLETE/FAIL/ABORT） */
    private OpenLineageEventType eventType;

    /** 事件时间 */
    private LocalDateTime eventTime;

    /** Run 标识（run.runId） */
    private String runId;

    /** 作业命名空间（job.namespace） */
    private String jobNamespace;

    /** 作业名（job.name） */
    private String jobName;

    /** 输入数据集 */
    @Builder.Default
    private List<OpenLineageDataset> inputs = new ArrayList<>();

    /** 输出数据集 */
    @Builder.Default
    private List<OpenLineageDataset> outputs = new ArrayList<>();
}
