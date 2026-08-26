package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.datamiddle.dqinsight.domain.gateway.ChannelGateway;
import com.yss.datamiddle.dqinsight.domain.model.ChannelState;
import com.yss.datamiddle.dqinsight.domain.model.ChannelType;
import com.yss.datamiddle.dqinsight.domain.model.IngestionChannel;
import com.yss.datamiddle.dqinsight.repository.DqBatchRepository;
import com.yss.datamiddle.dqinsight.repository.DqChannelRepository;
import com.yss.datamiddle.dqinsight.infrastructure.convertor.DqChannelConvertor;
import com.yss.datamiddle.dqinsight.repository.entity.DqBatchPO;
import com.yss.datamiddle.dqinsight.repository.entity.DqChannelPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 通道仓储实现（dq_channel；name 未删除唯一约束兜底并发 → 409 name-conflict；
 * 删除引用检查 dq_batch.channel_id → 409 in-use；updated_at 乐观并发版本位，C25）。
 */
@Repository
@RequiredArgsConstructor
public class ChannelGatewayImpl implements ChannelGateway {

    private final DqChannelRepository dqChannelRepository;
    private final DqBatchRepository dqBatchRepository;
    private final DqChannelConvertor dqChannelConvertor;

    @Override
    public IngestionChannel save(IngestionChannel channel) {
        DqChannelPO po = dqChannelConvertor.toPO(channel);
        dqChannelRepository.insert(po);
        channel.setId(po.getId());
        return channel;
    }

    @Override
    public Optional<IngestionChannel> findById(Long id) {
        DqChannelPO po = dqChannelRepository.selectById(id);
        if (po == null || po.getDeletedAt() != null) {
            return Optional.empty();
        }
        return Optional.of(dqChannelConvertor.toDomain(po));
    }

    @Override
    public IngestionChannel update(IngestionChannel channel) {
        DqChannelPO po = dqChannelConvertor.toPO(channel);
        dqChannelRepository.updateById(po);
        return channel;
    }

    @Override
    public List<IngestionChannel> listAll() {
        List<DqChannelPO> pos = dqChannelRepository.selectList(Wrappers.<DqChannelPO>lambdaQuery()
                .isNull(DqChannelPO::getDeletedAt)
                .orderByDesc(DqChannelPO::getCreatedAt));
        return pos.stream().map(dqChannelConvertor::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<IngestionChannel> listEnabledScheduledPull() {
        List<DqChannelPO> pos = dqChannelRepository.selectList(Wrappers.<DqChannelPO>lambdaQuery()
                .isNull(DqChannelPO::getDeletedAt)
                .eq(DqChannelPO::getType, ChannelType.SCHEDULED_PULL.getCode())
                .eq(DqChannelPO::getState, ChannelState.ENABLED.getCode()));
        return pos.stream().map(dqChannelConvertor::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean hasHistoricalResults(Long channelId) {
        LambdaQueryWrapper<DqBatchPO> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DqBatchPO::getChannelId, String.valueOf(channelId));
        return dqBatchRepository.selectCount(wrapper) > 0;
    }

    @Override
    public void delete(Long channelId) {
        dqChannelRepository.deleteById(channelId);
    }
}
