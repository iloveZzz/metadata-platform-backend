package com.yss.datamiddle.dqinsight.core.service;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.dqinsight.client.dto.LinkageMapDTO;
import com.yss.datamiddle.dqinsight.client.dto.query.PendingLinkagePageQuery;
import com.yss.datamiddle.dqinsight.client.vo.LinkageResultVO;
import com.yss.datamiddle.dqinsight.client.vo.PendingLinkageVO;

/**
 * 资产关联治理应用服务（DQI-006 / SB-05，Application 只编排，C10）。
 *
 * <p>用例：待关联队列分页（空队列以空分页表达）；人工映射（防腐层校验目标资产 → 已关联覆盖确认 →
 * 保存资产快照 → 触发健康分首次计算（复用切片 02 计算入口）→ 审计 linkage-map）。</p>
 */
public interface LinkageAppService {

    /**
     * 待关联资产队列（未命中，结果已入库；空队列以空分页表达，非错误）。
     */
    PageResult<PendingLinkageVO> listPending(PendingLinkagePageQuery query);

    /**
     * 人工映射（目标资产不存在 422 err.dq.asset.not-found；已关联未确认 409 err.dq.linkage.already-linked；
     * confirmOverwrite=true 二次确认覆盖；映射后触发健康分首次计算）。
     */
    LinkageResultVO mapLinkage(Long linkageId, LinkageMapDTO dto, String operator);
}
