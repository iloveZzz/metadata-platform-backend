package com.yss.datamiddle.aicontextlayer.domain.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 血缘图有向边领域对象（SEC-02 悬空边过滤）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineageEdge implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fromId;
    private String toId;
    private String confidence;
    private Provenance provenance;
}
