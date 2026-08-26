package com.yss.datamiddle.dqinsight.domain.gateway;

import com.yss.cloud.dto.page.PageQuery;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.dqinsight.client.dto.query.IngestionRecordPageQuery;
import com.yss.datamiddle.dqinsight.client.vo.IngestionRecordVO;
import com.yss.datamiddle.dqinsight.domain.model.AssetLinkage;
import com.yss.datamiddle.dqinsight.domain.model.DQResultBatch;
import com.yss.datamiddle.dqinsight.domain.model.RuleResultRow;

import java.util.List;

/**
 * DQ 结果仓储端口（Domain 定义，Infrastructure 实现）。
 *
 * <p>保存 = 批次 + 规则明细 + 关联（pending）单聚合事务（Application 用例边界）；
 * 幂等由 dq_batch UNIQUE(source_tool, batch_no) 唯一约束兜底并发（C20），禁止先查后插。</p>
 */
public interface DqResultGateway {

    /**
     * 保存批次（批次 + 规则明细 + 关联单聚合事务；解析成功即入库，与关联解耦）。
     *
     * @param batch    批次（status = ingested / parse-failed）
     * @param rows     规则明细（parse-failed 为空）
     * @param linkages 资产关联（pending / linked）
     * @return 已保存批次（含分配的 id）
     */
    DQResultBatch save(DQResultBatch batch, List<RuleResultRow> rows, List<AssetLinkage> linkages);

    /**
     * 接入记录分页查询（sourceTool / channelId / status / linkageStatus 筛选；PageQuery 自动分页，
     * 总数经 query.tempTotalCount 回读；0 条以空分页表达）。
     */
    List<IngestionRecordVO> listIngestionRecords(IngestionRecordPageQuery query);

    /**
     * 按 ID 查询批次（健康分计算服务读取执行时间 / 有效期；不存在返回 null）。
     */
    DQResultBatch findBatchById(Long batchId);

    /**
     * 按批次查询规则结果（健康分计算输入，切片 02 消费）。
     */
    List<RuleResultRow> findRuleResultsByBatchId(Long batchId);

    /**
     * 按批次查询资产关联（健康分计算取关联命中资产的名称 / 域 / 类型快照）。
     */
    List<AssetLinkage> findLinkagesByBatchId(Long batchId);

    /**
     * 组装分页结果（PageQuery 自动分页的 total 在查询后回读）。
     */
    static PageResult<IngestionRecordVO> toPage(List<IngestionRecordVO> records, PageQuery query) {
        return PageResult.of(records, query.getTempTotalCount(), query.getPageSize(), query.getPageIndex());
    }
}
