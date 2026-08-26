package com.yss.datamiddle.dqinsight.domain.exception;

import com.yss.datamiddle.dqinsight.domain.constant.DqErrorCodes;

/**
 * 通道重名（409 err.dq.channel.name-conflict，name 未删除唯一约束兜底）。
 */
public class ChannelNameConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ChannelNameConflictException(String name) {
        super("通道名已存在：name=" + name);
    }

    public String getErrCode() {
        return DqErrorCodes.CHANNEL_NAME_CONFLICT;
    }
}
