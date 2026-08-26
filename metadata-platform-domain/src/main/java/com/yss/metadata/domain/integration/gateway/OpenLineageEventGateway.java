package com.yss.metadata.domain.integration.gateway;

import com.yss.metadata.domain.integration.model.OpenLineageEventRecord;
import com.yss.metadata.domain.integration.model.OpenLineageStats;

/**
 * OpenLineage 事件记录仓储端口（集成域；Domain 定义，Infrastructure 实现）。
 *
 * <p>openlineage_event 表：事件追加记录（统计依据）+ 近 24h/解析成功率聚合。</p>
 */
public interface OpenLineageEventGateway {

    /**
     * 追加事件接收记录（不可变，追加式）。
     */
    void save(OpenLineageEventRecord record);

    /**
     * 事件统计（近 24h 事件数 + 解析成功率；0 事件空结构非错误）。
     */
    OpenLineageStats stats();
}
