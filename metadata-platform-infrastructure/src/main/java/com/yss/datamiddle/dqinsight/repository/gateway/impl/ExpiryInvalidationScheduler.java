package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.yss.datamiddle.dqinsight.domain.gateway.BatchExpiryGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 过期流转低频调度（进程内调度器，系统自动，非用户动作；OQ-03 / C23）。
 *
 * <p>间隔可配置（`dq.expiry.scheduler.interval-ms`，默认 1 小时）；批量 UPDATE 幂等可重跑
 * （BAC 风险 / 回滚约束：进程内轻量调度器）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiryInvalidationScheduler {

    private final BatchExpiryGateway batchExpiryGateway;

    @Scheduled(fixedDelayString = "${dq.expiry.scheduler.interval-ms:3600000}")
    public void invalidateExpiredBatches() {
        try {
            int affected = batchExpiryGateway.invalidateExpired(Instant.now());
            if (affected > 0) {
                log.info("过期流转：{} 个批次已置 invalidated", affected);
            }
        } catch (RuntimeException e) {
            log.error("过期流转执行失败（下次调度重试）", e);
        }
    }
}
