package com.yss.datamiddle.dqinsight.domain.exception;

import com.yss.datamiddle.dqinsight.domain.constant.DqErrorCodes;

/**
 * 资产关联不存在（404 err.dq.not-found）。
 */
public class LinkageNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LinkageNotFoundException(Long linkageId) {
        super("资产关联不存在：linkageId=" + linkageId);
    }

    public String getErrCode() {
        return DqErrorCodes.NOT_FOUND;
    }
}
