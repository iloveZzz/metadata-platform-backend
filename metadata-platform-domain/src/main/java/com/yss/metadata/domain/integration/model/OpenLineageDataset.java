package com.yss.metadata.domain.integration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * OpenLineage 数据集（冻结 API OpenLineageDataset：namespace + name；facets 暂不解析）。
 *
 * <p>事件数据集映射为资产（source_id=namespace、name=dataset name）与血缘边端点；
 * facets 解析完备化 seam-deferred（列级血缘/格式 facet 随事件量增长重估）。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenLineageDataset implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据集命名空间（映射资产 source_id） */
    private String namespace;

    /** 数据集名称（映射资产 name） */
    private String name;
}
