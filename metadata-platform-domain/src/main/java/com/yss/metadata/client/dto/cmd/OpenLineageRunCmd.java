package com.yss.metadata.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * OpenLineage Run 标识（OpenLineageEventCmd 内嵌：run.runId）。
 */
@Getter
@Setter
public class OpenLineageRunCmd extends CommandDTO {

    private static final long serialVersionUID = 1L;

    /** Run 标识 */
    private String runId;
}
