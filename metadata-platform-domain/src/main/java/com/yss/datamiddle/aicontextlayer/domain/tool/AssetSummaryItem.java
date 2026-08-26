package com.yss.datamiddle.aicontextlayer.domain.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 资产搜索摘要领域对象（契约 3.1）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetSummaryItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String type;
    private String domain;
    private String classification;
    private Provenance provenance;
}
