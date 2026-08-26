package com.yss.datamiddle.dqinsight.domain.exception;

import com.yss.datamiddle.dqinsight.domain.model.ErrorCategory;
import com.yss.datamiddle.dqinsight.client.vo.FieldErrorItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 接入解析失败异常（422 字段级错误，错误分类 format / auth / network）。
 *
 * <p>错误信息脱敏：只携带错误码、错误分类与字段路径，不泄露请求内容与凭证。</p>
 */
public class IngestValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errCode;

    private final ErrorCategory errorCategory;

    private final List<FieldErrorItem> fieldErrors;

    public IngestValidationException(String errCode, ErrorCategory errorCategory, String message,
            List<FieldErrorItem> fieldErrors) {
        super(message);
        this.errCode = errCode;
        this.errorCategory = errorCategory;
        this.fieldErrors = fieldErrors == null ? new ArrayList<>() : fieldErrors;
    }

    public IngestValidationException(String errCode, ErrorCategory errorCategory, String message) {
        this(errCode, errorCategory, message, Collections.emptyList());
    }

    public String getErrCode() {
        return errCode;
    }

    public ErrorCategory getErrorCategory() {
        return errorCategory;
    }

    public List<FieldErrorItem> getFieldErrors() {
        return fieldErrors;
    }
}
