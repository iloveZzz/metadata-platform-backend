package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 集成配置视图对象（冻结 OpenAPI GET /api/integrations 响应 data：
 * Gravitino 上游 + DataHub 导出目标 + OpenLineage 接收统计）。
 *
 * <p>生成类型 IntegrationResponse 的 data 为无属性 object，前端经 unknown 桥接为本地类型。</p>
 */
@Getter
@Setter
public class IntegrationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Gravitino 上游配置 */
    private GravitinoConfigVO gravitino;

    /** DataHub 导出目标配置 */
    private DataHubConfigVO datahub;

    /** OpenLineage 接收统计 */
    private OpenLineageConfigVO openLineage;
}
