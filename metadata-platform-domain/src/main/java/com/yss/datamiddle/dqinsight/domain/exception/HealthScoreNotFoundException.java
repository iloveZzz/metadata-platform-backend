package com.yss.datamiddle.dqinsight.domain.exception;

import com.yss.datamiddle.dqinsight.domain.constant.DqErrorCodes;

/**
 * 健康分不存在（404 err.dq.not-found；健康分详情 / 规则明细钻取资产无健康分时）。
 */
public class HealthScoreNotFoundException extends RuntimeException {

    private final String errCode;

    public HealthScoreNotFoundException(String assetId) {
        super("资产健康分不存在：" + assetId);
        this.errCode = DqErrorCodes.NOT_FOUND;
    }

    public String getErrCode() {
        return errCode;
    }
}
