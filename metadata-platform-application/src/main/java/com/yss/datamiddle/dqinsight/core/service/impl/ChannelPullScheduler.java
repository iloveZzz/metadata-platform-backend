package com.yss.datamiddle.dqinsight.core.service.impl;

import com.yss.datamiddle.dqinsight.core.service.ChannelAppService;
import com.yss.datamiddle.dqinsight.domain.gateway.ChannelGateway;
import com.yss.datamiddle.dqinsight.domain.model.IngestionChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 定时拉取调度器（进程内轻量，BAC 风险 / 回滚约束：幂等可重跑，人工确认项）。
 *
 * <p>固定间隔扫描启用的定时拉取通道，按通道 cron 周期判定到点后触发拉取（复用切片 01 接入管线，
 * 合同 seam）；拉取中 / 已停用由 ChannelAppService 幂等跳过，不抛 409。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelPullScheduler {

    private final ChannelGateway channelGateway;
    private final ChannelAppService channelAppService;

    @Scheduled(fixedDelayString = "${dq.pull.scan-ms:60000}")
    public void scanDuePulls() {
        List<IngestionChannel> channels = channelGateway.listEnabledScheduledPull();
        for (IngestionChannel channel : channels) {
            if (cronMatches(channel.getSchedule(), LocalDateTime.now())) {
                log.info("定时拉取触发: channelId={}, name={}", channel.getId(), channel.getName());
                try {
                    channelAppService.runScheduledPull(channel.getId());
                } catch (Exception e) {
                    // 单通道失败不阻断其余通道（调度器幂等可重跑）
                    log.warn("定时拉取异常（已记录，不阻断其余通道）: channelId={}", channel.getId(), e);
                }
            }
        }
    }

    private static boolean cronMatches(String cron, LocalDateTime now) {
        if (cron == null || cron.trim().isEmpty()) {
            return false;
        }
        try {
            CronExpression expression = CronExpression.parse(cron.trim());
            LocalDateTime next = expression.next(now.minusSeconds(1));
            return next != null && next.isBefore(now.plusSeconds(1));
        } catch (IllegalArgumentException e) {
            log.warn("通道拉取周期 cron 非法（按不到点处理）: cron={}", cron);
            return false;
        }
    }
}
