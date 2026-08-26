package com.yss.metadata.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * OpenLineage 作业标识（OpenLineageEventCmd 内嵌：job.namespace + job.name）。
 */
@Getter
@Setter
public class OpenLineageJobCmd extends CommandDTO {

    private static final long serialVersionUID = 1L;

    /** 作业命名空间 */
    private String namespace;

    /** 作业名 */
    private String name;
}
