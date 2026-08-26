package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.datamiddle.dqinsight.domain.gateway.BatchExpiryGateway;
import com.yss.datamiddle.dqinsight.domain.model.IngestionStatus;
import com.yss.datamiddle.dqinsight.repository.DqBatchRepository;
import com.yss.datamiddle.dqinsight.repository.entity.DqBatchPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 批次过期流转实现（OQ-03 系统自动流转，非用户动作；数据架构 §7/§8）。
 *
 * <p>低频调度将 valid_until &lt; now 且 status = ingested 的批次置 invalidated；
 * 幂等可重跑（第二次执行匹配 0 行）。</p>
 */
@Repository
@RequiredArgsConstructor
public class BatchExpiryGatewayImpl implements BatchExpiryGateway {

    private final DqBatchRepository dqBatchRepository;

    @Override
    public int invalidateExpired(Instant now) {
        LocalDateTime nowLdt = now == null ? LocalDateTime.now() : LocalDateTime.ofInstant(now, ZoneId.systemDefault());
        LambdaUpdateWrapper<DqBatchPO> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(DqBatchPO::getStatus, IngestionStatus.INGESTED.getCode())
                .lt(DqBatchPO::getValidUntil, nowLdt)
                .set(DqBatchPO::getStatus, IngestionStatus.INVALIDATED.getCode());
        return dqBatchRepository.update(null, wrapper);
    }
}
