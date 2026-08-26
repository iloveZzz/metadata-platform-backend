package com.yss.datamiddle.dqinsight.core.service;

import com.yss.datamiddle.dqinsight.client.dto.ChannelCreateDTO;
import com.yss.datamiddle.dqinsight.client.dto.ChannelUpdateDTO;
import com.yss.datamiddle.dqinsight.client.vo.ChannelVO;

import java.util.List;

/**
 * 通道管理应用服务（Application 只编排；状态机领域规则在 Domain，C10）。
 *
 * <p>用例：新建（重名 409 name-conflict）/ 更新与启停（拉取中 409 busy，停用二次确认由前端交互承担）/
 * 删除（历史结果 409 in-use）/ 重试拉取（幂等，复用切片 01 管线）/ 定时拉取触发；
 * 凭证加密存储（密文不回传）；审计 channel-config / channel-toggle / channel-retry（SB-08）。</p>
 */
public interface ChannelAppService {

    /**
     * 通道列表（未删除，创建时间倒序；密文不回传）。
     */
    List<ChannelVO> listChannels();

    /**
     * 通道详情（不存在 404 err.dq.not-found）。
     */
    ChannelVO getChannel(Long id);

    /**
     * 新建通道（name 重名 409；scheduled-pull 时 schedule 必填 422）。
     */
    ChannelVO createChannel(ChannelCreateDTO dto, String operator);

    /**
     * 更新通道（至少一个字段 422；拉取中 409 busy；凭证重设加密存储）。
     */
    ChannelVO updateChannel(Long id, ChannelUpdateDTO dto, String operator);

    /**
     * 删除通道（存在历史接入结果 409 in-use；不可逆）。
     */
    void deleteChannel(Long id, String operator);

    /**
     * 重试拉取（拉取失败 / 启用 → 拉取中 → 成功 / 失败；拉取中重复触发 409 busy 幂等）。
     */
    ChannelVO retryPull(Long id, String operator);

    /**
     * 定时拉取触发（调度器调用；拉取中跳过，不抛 409）。
     */
    void runScheduledPull(Long channelId);
}
