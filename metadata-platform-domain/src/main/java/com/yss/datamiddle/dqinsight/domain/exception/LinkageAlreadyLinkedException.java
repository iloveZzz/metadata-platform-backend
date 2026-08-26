package com.yss.datamiddle.dqinsight.domain.exception;

import com.yss.datamiddle.dqinsight.domain.constant.DqErrorCodes;

/**
 * 关联批次已关联（409 err.dq.linkage.already-linked：覆盖需 confirmOverwrite=true + 二次确认，C26）。
 */
public class LinkageAlreadyLinkedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LinkageAlreadyLinkedException(Long linkageId) {
        super("该批次资产已关联：linkageId=" + linkageId + "（覆盖需 confirmOverwrite=true + 二次确认）");
    }

    public String getErrCode() {
        return DqErrorCodes.LINKAGE_ALREADY_LINKED;
    }
}
