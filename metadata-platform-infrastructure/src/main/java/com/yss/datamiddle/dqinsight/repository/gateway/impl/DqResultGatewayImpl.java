package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.dqinsight.client.dto.query.IngestionRecordPageQuery;
import com.yss.datamiddle.dqinsight.client.vo.IngestionRecordVO;
import com.yss.datamiddle.dqinsight.domain.gateway.DqResultGateway;
import com.yss.datamiddle.dqinsight.domain.model.AssetLinkage;
import com.yss.datamiddle.dqinsight.domain.model.DQResultBatch;
import com.yss.datamiddle.dqinsight.domain.model.RuleResultRow;
import com.yss.datamiddle.dqinsight.repository.DqAssetLinkageRepository;
import com.yss.datamiddle.dqinsight.repository.DqBatchRepository;
import com.yss.datamiddle.dqinsight.repository.DqRuleResultRepository;
import com.yss.datamiddle.dqinsight.infrastructure.convertor.DqAssetLinkageConvertor;
import com.yss.datamiddle.dqinsight.infrastructure.convertor.DqBatchConvertor;
import com.yss.datamiddle.dqinsight.infrastructure.convertor.DqRuleResultConvertor;
import com.yss.datamiddle.dqinsight.repository.entity.DqAssetLinkagePO;
import com.yss.datamiddle.dqinsight.repository.entity.DqBatchPO;
import com.yss.datamiddle.dqinsight.repository.entity.DqRuleResultPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DQ 结果仓储实现（批次 + 规则明细 + 关联单聚合事务；接入记录分页查询）。
 *
 * <p>幂等去重不先查后插：直接 INSERT，UNIQUE(source_tool, batch_no) 唯一约束兜底并发（C20）。</p>
 */
@Repository
@RequiredArgsConstructor
public class DqResultGatewayImpl implements DqResultGateway {

    private final DqBatchRepository dqBatchRepository;
    private final DqRuleResultRepository dqRuleResultRepository;
    private final DqAssetLinkageRepository dqAssetLinkageRepository;
    private final DqBatchConvertor dqBatchConvertor;
    private final DqRuleResultConvertor dqRuleResultConvertor;
    private final DqAssetLinkageConvertor dqAssetLinkageConvertor;

    @Override
    public DQResultBatch save(DQResultBatch batch, List<RuleResultRow> rows, List<AssetLinkage> linkages) {
        DqBatchPO batchPO = dqBatchConvertor.toPO(batch);
        dqBatchRepository.insert(batchPO);
        batch.assignId(batchPO.getId());

        if (rows != null && !rows.isEmpty()) {
            List<DqRuleResultPO> rowPOs = new ArrayList<>(rows.size());
            for (RuleResultRow row : rows) {
                DqRuleResultPO po = dqRuleResultConvertor.toPO(row);
                po.setId(IdWorker.getId());
                po.setBatchId(batchPO.getId());
                rowPOs.add(po);
            }
            dqRuleResultRepository.insertBatchSomeColumn(rowPOs);
        }

        if (linkages != null && !linkages.isEmpty()) {
            List<DqAssetLinkagePO> linkagePOs = new ArrayList<>(linkages.size());
            for (AssetLinkage linkage : linkages) {
                DqAssetLinkagePO po = dqAssetLinkageConvertor.toPO(linkage);
                po.setId(IdWorker.getId());
                po.setBatchId(batchPO.getId());
                linkagePOs.add(po);
            }
            dqAssetLinkageRepository.insertBatchSomeColumn(linkagePOs);
        }
        return batch;
    }

    @Override
    public List<IngestionRecordVO> listIngestionRecords(IngestionRecordPageQuery query) {
        LambdaQueryWrapper<DqBatchPO> wrapper = Wrappers.lambdaQuery();
        if (query.getSourceTool() != null) {
            wrapper.eq(DqBatchPO::getSourceTool, query.getSourceTool().getCode());
        }
        if (query.getChannelId() != null && !query.getChannelId().isEmpty()) {
            wrapper.eq(DqBatchPO::getChannelId, query.getChannelId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(DqBatchPO::getStatus, query.getStatus().getCode());
        }
        if (query.getLinkageStatus() != null) {
            wrapper.eq(DqBatchPO::getLinkageStatus, query.getLinkageStatus().getCode());
        }
        wrapper.orderByDesc(DqBatchPO::getReceivedAt);

        com.baomidou.mybatisplus.core.metadata.IPage<DqBatchPO> page = dqBatchRepository.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(query.getPageIndex(), query.getPageSize()), wrapper);
        query.setTempTotalCount(page.getTotal());
        return page.getRecords().stream()
                .map(dqBatchConvertor::toIngestionRecordVO)
                .collect(Collectors.toList());
    }

    @Override
    public DQResultBatch findBatchById(Long batchId) {
        DqBatchPO po = dqBatchRepository.selectById(batchId);
        return po == null ? null : dqBatchConvertor.toDomain(po);
    }

    @Override
    public List<RuleResultRow> findRuleResultsByBatchId(Long batchId) {
        LambdaQueryWrapper<DqRuleResultPO> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DqRuleResultPO::getBatchId, batchId);
        List<DqRuleResultPO> pos = dqRuleResultRepository.selectList(wrapper);
        return pos.stream()
                .map(dqRuleResultConvertor::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetLinkage> findLinkagesByBatchId(Long batchId) {
        LambdaQueryWrapper<DqAssetLinkagePO> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DqAssetLinkagePO::getBatchId, batchId);
        List<DqAssetLinkagePO> pos = dqAssetLinkageRepository.selectList(wrapper);
        return pos.stream()
                .map(dqAssetLinkageConvertor::toDomain)
                .collect(Collectors.toList());
    }
}
