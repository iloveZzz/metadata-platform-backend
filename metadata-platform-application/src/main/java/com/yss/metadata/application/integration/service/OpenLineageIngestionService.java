package com.yss.metadata.application.integration.service;

import com.yss.metadata.client.dto.cmd.OpenLineageEventCmd;

/**
 * OpenLineage 事件接收应用服务（FR-011/019；WU-05-02）。
 *
 * <p>POST /api/v1/lineage：RunEvent 子集校验（422）→ 事件记录 →
 * 数据集映射资产（source_id=namespace、name=dataset name）+ 血缘边
 * （COMPLETE 事件 inputs→outputs，type=job，confidence=auto-high）。
 * 事件接收为 202 语义（请求内同步执行，异步化随切片 05 重估）。</p>
 */
public interface OpenLineageIngestionService {

    /**
     * 接收并处理 OpenLineage 事件（校验失败抛非法参数 → 422）。
     */
    void receive(OpenLineageEventCmd cmd);
}
