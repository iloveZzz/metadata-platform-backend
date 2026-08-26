package com.yss.datamiddle.dqinsight.domain.exception;

import com.yss.datamiddle.dqinsight.domain.constant.DqErrorCodes;

/**
 * 通道删除引用冲突（409 err.dq.channel.in-use：存在历史接入结果，结果引用必须可追溯，数据架构 §2）。
 */
public class ChannelInUseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ChannelInUseException(Long channelId) {
        super("通道存在历史接入结果，不可删除：id=" + channelId);
    }

    public String getErrCode() {
        return DqErrorCodes.CHANNEL_IN_USE;
    }
}
