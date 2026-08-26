package com.yss.datamiddle.dqinsight.domain.exception;

import com.yss.datamiddle.dqinsight.domain.constant.DqErrorCodes;

/**
 * 无权限（403 err.dq.forbidden；数据域外不可见，DQI-007）。
 *
 * <p>本切片（01）无权限判定生产路径（域过滤属切片 05），保留异常与统一错误体映射供横切接入。</p>
 */
public class DqForbiddenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DqForbiddenException(String message) {
        super(message);
    }

    public String getErrCode() {
        return DqErrorCodes.FORBIDDEN;
    }
}
