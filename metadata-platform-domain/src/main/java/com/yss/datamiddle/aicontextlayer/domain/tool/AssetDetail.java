package com.yss.datamiddle.aicontextlayer.domain.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 资产详情领域对象（SEC-04 / 契约 3.2）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String type;
    private String domain;
    private String classification;
    private List<AssetColumnItem> columns;
    private Provenance provenance;
}
