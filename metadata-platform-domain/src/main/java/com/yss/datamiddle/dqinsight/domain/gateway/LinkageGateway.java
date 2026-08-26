package com.yss.datamiddle.dqinsight.domain.gateway;

import com.yss.cloud.dto.page.PageQuery;
import com.yss.datamiddle.dqinsight.client.vo.PendingLinkageVO;
import com.yss.datamiddle.dqinsight.domain.model.AssetLinkage;

import java.util.List;

/**
 * 资产关联治理端口（待关联队列 / 人工映射持久化，DQI-006 / SB-05）。
 *
 * <p>pending 队列 = dq_asset_linkage.state = pending（未命中，结果已入库，不阻断验收）；
 * 人工映射 UPDATE resolved_asset_id + 资产快照 + state = linked，随后触发健康分首次计算（切片 02 seam）。</p>
 */
public interface LinkageGateway {

    /**
     * 待关联队列分页（PageQuery 自动分页，总数经 query.tempTotalCount 回读；空队列以空分页表达）。
     */
    List<PendingLinkageVO> listPending(PageQuery query);

    /**
     * 按 ID 查询资产关联（不存在返回 null → 404 err.dq.not-found）。
     */
    AssetLinkage findById(Long id);

    /**
     * 更新资产关联（人工映射后保存；单聚合事务边界在 Application 用例）。
     */
    void save(AssetLinkage linkage);
}
