package com.yss.datamiddle.dqinsight.domain.gateway;

import com.yss.datamiddle.dqinsight.domain.model.IngestionChannel;
import com.yss.datamiddle.dqinsight.domain.model.PullOutcome;

/**
 * 通道拉取执行端口（定时拉取 / 手动重试统一入口）。
 *
 * <p>拉取执行复用切片 01 接入管线（合同 seam_deferred：调度器为进程内实现（人工确认项），
 * 拉取执行依赖切片 01 管线 seam）；取数目标为外部 DQ 工具（MVP 拉取地址由部署配置
 * dq.pull.base-url 提供，契约未定义 URL——切片 04 人工审查点 / P1 与部署联调定稿）。</p>
 */
public interface ChannelPullPort {

    /**
     * 执行一次拉取（成功 = 已复用切片 01 管线入库；失败 = 分类 format / auth / network + 脱敏信息）。
     */
    PullOutcome pull(IngestionChannel channel);
}
