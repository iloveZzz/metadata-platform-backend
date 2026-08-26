package com.yss.datamiddle.aicontextlayer.domain.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 血缘图节点领域对象（SEC-02 / SEC-04）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineageNode implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String type;
    private String domain;
    private String classification;
    private Provenance provenance;
}
