package com.yss.datamiddle.dqinsight.domain.exception;

import com.yss.datamiddle.dqinsight.domain.constant.DqErrorCodes;

/**
 * 通道不存在（404 err.dq.not-found）。
 */
public class ChannelNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ChannelNotFoundException(Long channelId) {
        super("通道不存在：id=" + channelId);
    }

    public String getErrCode() {
        return DqErrorCodes.NOT_FOUND;
    }
}
