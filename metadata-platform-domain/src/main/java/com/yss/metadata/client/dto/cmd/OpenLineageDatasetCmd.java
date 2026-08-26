package com.yss.metadata.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * OpenLineage 数据集（OpenLineageEventCmd 内嵌：namespace + name）。
 */
@Getter
@Setter
public class OpenLineageDatasetCmd extends CommandDTO {

    private static final long serialVersionUID = 1L;

    /** 数据集命名空间（映射资产 source_id） */
    private String namespace;

    /** 数据集名称（映射资产 name） */
    private String name;
}
