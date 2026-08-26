package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * OpenLineage 接收配置视图（IntegrationVO.openLineage：端点只读展示 + 事件统计）。
 */
@Getter
@Setter
public class OpenLineageConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 接收端点（只读展示，自身路径 /api/v1/lineage） */
    private String receiveEndpoint;

    /** 近 24h 接收事件数 */
    private long recent24h;

    /** 解析成功率（百分比字符串，如 97.3%；0 事件为空） */
    private String parseSuccessRate;
}
