package com.yss.metadata.domain.integration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * OpenLineage 事件统计（集成配置页展示：近 24h 事件数 + 解析成功率）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenLineageStats implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 近 24h 接收事件数 */
    private long recent24hCount;

    /** 解析成功率（0~1；无事件时 0） */
    private double parseSuccessRate;
}
