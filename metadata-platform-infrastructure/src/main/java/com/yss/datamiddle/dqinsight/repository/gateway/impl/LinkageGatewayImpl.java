package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.cloud.dto.page.PageQuery;
import com.yss.datamiddle.dqinsight.client.dto.query.PendingLinkagePageQuery;
import com.yss.datamiddle.dqinsight.client.vo.PendingLinkageVO;
import com.yss.datamiddle.dqinsight.domain.gateway.LinkageGateway;
import com.yss.datamiddle.dqinsight.domain.model.AssetLinkage;
import com.yss.datamiddle.dqinsight.domain.model.LinkageState;
import com.yss.datamiddle.dqinsight.domain.model.SourceTool;
import com.yss.datamiddle.dqinsight.domain.util.IsoTimes;
import com.yss.datamiddle.dqinsight.repository.DqAssetLinkageRepository;
import com.yss.datamiddle.dqinsight.repository.DqBatchRepository;
import com.yss.datamiddle.dqinsight.repository.DqChannelRepository;
import com.yss.datamiddle.dqinsight.infrastructure.convertor.DqAssetLinkageConvertor;
import com.yss.datamiddle.dqinsight.repository.entity.DqAssetLinkagePO;
import com.yss.datamiddle.dqinsight.repository.entity.DqBatchPO;
import com.yss.datamiddle.dqinsight.repository.entity.DqChannelPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 资产关联治理实现（pending 队列分页 + 人工映射持久化，DQI-006 / SB-05）。
 *
 * <p>pending 队列 = dq_asset_linkage.state = pending（未命中，结果已入库，不阻断验收）；
 * 队列条目 join dq_batch（batchNo / sourceTool / receivedAt / rowCount）；空队列以空分页表达。
 * 切片 05 追加数据域过滤（C24）：待关联资产归属 = 来源通道域（batch.channel_id →
 * dq_channel.domain，结果来源口径）；可见域为空 = 不限制；受限用户对域不可判定
 * （无通道 / 通道无域）的记录按域外隐藏（不泄露存在性，人工审查点见 05 证据）。</p>
 */
@Repository
@RequiredArgsConstructor
public class LinkageGatewayImpl implements LinkageGateway {

    private final DqAssetLinkageRepository dqAssetLinkageRepository;
    private final DqBatchRepository dqBatchRepository;
    private final DqChannelRepository dqChannelRepository;
    private final DqAssetLinkageConvertor dqAssetLinkageConvertor;

    @Override
    public List<PendingLinkageVO> listPending(PageQuery query) {
        LambdaQueryWrapper<DqAssetLinkagePO> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DqAssetLinkagePO::getState, LinkageState.PENDING.getCode())
                .orderByDesc(DqAssetLinkagePO::getCreatedAt);
        applyDomainFilter(wrapper, query);
        com.baomidou.mybatisplus.core.metadata.IPage<DqAssetLinkagePO> page = dqAssetLinkageRepository.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(query.getPageIndex(), query.getPageSize()), wrapper);
        query.setTempTotalCount(page.getTotal());
        List<DqAssetLinkagePO> pos = page.getRecords();

        Map<Long, DqBatchPO> batches = loadBatches(pos);
        List<PendingLinkageVO> vos = new ArrayList<>(pos.size());
        for (DqAssetLinkagePO po : pos) {
            DqBatchPO batch = batches.get(po.getBatchId());
            PendingLinkageVO vo = new PendingLinkageVO();
            vo.setId(String.valueOf(po.getId()));
            vo.setAssetId(po.getSourceAssetId());
            vo.setBatchNo(batch == null ? null : batch.getBatchNo());
            vo.setSourceTool(batch == null ? null : SourceTool.fromCodeOrNull(batch.getSourceTool()));
            vo.setReceivedAt(batch == null ? null : IsoTimes.format(toInstant(batch.getReceivedAt())));
            vo.setRowCount(batch == null ? null : batch.getRowCount());
            vo.setNote(po.getNote());
            vos.add(vo);
        }
        return vos;
    }

    /**
     * 数据域过滤（C24）：受限用户仅可见来源通道域 ∈ 可见域的 pending 记录；
     * 通道不存在 / 无域（域不可判定）按域外隐藏。过滤在 SQL 层完成，保证自动分页语义正确。
     */
    private void applyDomainFilter(LambdaQueryWrapper<DqAssetLinkagePO> wrapper, PageQuery query) {
        if (!(query instanceof PendingLinkagePageQuery)) {
            return;
        }
        List<String> visible = ((PendingLinkagePageQuery) query).getVisibleDomains();
        if (visible == null || visible.isEmpty()) {
            return;
        }
        List<DqChannelPO> channels = dqChannelRepository.selectList(
                Wrappers.<DqChannelPO>lambdaQuery().in(DqChannelPO::getDomain, visible));
        Set<String> channelIds = channels.stream()
                .map(c -> String.valueOf(c.getId()))
                .collect(Collectors.toSet());
        if (channelIds.isEmpty()) {
            // 无可见通道 → 无可见 pending（恒 false，走查询以保持分页 totalCount 回读）
            wrapper.apply("1 = 0");
            return;
        }
        String channelIdList = String.join(",", channelIds);
        wrapper.inSql(DqAssetLinkagePO::getBatchId,
                "SELECT id FROM dq_batch WHERE channel_id IS NOT NULL AND channel_id IN (" + channelIdList + ")");
    }

    @Override
    public AssetLinkage findById(Long id) {
        DqAssetLinkagePO po = dqAssetLinkageRepository.selectById(id);
        return po == null ? null : dqAssetLinkageConvertor.toDomain(po);
    }

    @Override
    public void save(AssetLinkage linkage) {
        DqAssetLinkagePO po = dqAssetLinkageConvertor.toPO(linkage);
        dqAssetLinkageRepository.updateById(po);
    }

    private Map<Long, DqBatchPO> loadBatches(List<DqAssetLinkagePO> pos) {
        Set<Long> batchIds = pos.stream()
                .map(DqAssetLinkagePO::getBatchId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (batchIds.isEmpty()) {
            return new HashMap<>();
        }
        List<DqBatchPO> batches = dqBatchRepository.selectBatchIds(batchIds);
        Map<Long, DqBatchPO> byId = new HashMap<>();
        for (DqBatchPO batch : batches) {
            byId.put(batch.getId(), batch);
        }
        return byId;
    }

    private static Instant toInstant(java.time.LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }
}
