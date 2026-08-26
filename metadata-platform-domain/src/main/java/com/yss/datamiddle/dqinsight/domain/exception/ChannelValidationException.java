package com.yss.datamiddle.dqinsight.domain.exception;

import com.yss.datamiddle.dqinsight.client.vo.FieldErrorItem;

import java.util.List;

/**
 * 通道 / 映射请求校验失败（422，字段级错误）。
 */
public class ChannelValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errCode;

    private final List<FieldErrorItem> fieldErrors;

    public ChannelValidationException(String errCode, String message, List<FieldErrorItem> fieldErrors) {
        super(message);
        this.errCode = errCode;
        this.fieldErrors = fieldErrors;
    }

    public String getErrCode() {
        return errCode;
    }

    public List<FieldErrorItem> getFieldErrors() {
        return fieldErrors;
    }
}
