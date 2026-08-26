package com.yss.datamiddle.dqinsight.domain.exception;

import com.yss.datamiddle.dqinsight.domain.constant.DqErrorCodes;

/**
 * 通道状态冲突（409 err.dq.channel.busy：拉取中重复触发 / 拉取中更新配置与启停 / 停用通道重试，C25 幂等）。
 */
public class ChannelBusyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ChannelBusyException(Long channelId, String message) {
        super("通道状态冲突（id=" + channelId + "）：" + message);
    }

    public String getErrCode() {
        return DqErrorCodes.CHANNEL_BUSY;
    }
}
