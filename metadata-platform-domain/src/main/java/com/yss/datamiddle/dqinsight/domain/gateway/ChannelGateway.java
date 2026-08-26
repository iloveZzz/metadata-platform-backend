package com.yss.datamiddle.dqinsight.domain.gateway;

import com.yss.datamiddle.dqinsight.domain.model.IngestionChannel;

import java.util.List;
import java.util.Optional;

/**
 * 接入通道仓储端口（Domain 定义，Infrastructure 实现）。
 *
 * <p>name 未删除唯一（重名 409，唯一约束兜底并发，禁止先查后插）；删除存在历史接入结果 409
 * （dq_batch.channel_id 引用检查）；updated_at 乐观并发版本位（C25 状态机持久化）。</p>
 */
public interface ChannelGateway {

    /**
     * 保存通道（新建 INSERT；name 唯一约束冲突时抛 DuplicateKeyException → 409 name-conflict）。
     */
    IngestionChannel save(IngestionChannel channel);

    /**
     * 按 ID 查询通道（未删除；不存在返回 empty → 404 err.dq.not-found）。
     */
    Optional<IngestionChannel> findById(Long id);

    /**
     * 更新通道（updated_at 乐观并发；覆盖整行非空字段）。
     */
    IngestionChannel update(IngestionChannel channel);

    /**
     * 通道列表（未删除，创建时间倒序；通道量级小不分页，冻结契约 m7）。
     */
    List<IngestionChannel> listAll();

    /**
     * 启用的定时拉取通道（调度器扫描输入）。
     */
    List<IngestionChannel> listEnabledScheduledPull();

    /**
     * 通道是否存在历史接入结果（dq_batch.channel_id 引用；存在 → 删除 409 in-use）。
     */
    boolean hasHistoricalResults(Long channelId);

    /**
     * 删除通道（无历史结果时物理删除；有历史结果调用方先 409）。
     */
    void delete(Long channelId);
}
