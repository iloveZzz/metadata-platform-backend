package com.yss.metadata.rest.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一错误响应体（冻结 OpenAPI Error schema）。
 *
 * <p>字段：code（业务错误码）/ message / severity（error|warning）/ fieldErrors。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class ErrorResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务错误码（如 err.connector.network / connector.not_found） */
    private String code;

    /** 用户可读错误信息 */
    private String message;

    /** 严重级别（error / warning） */
    private String severity;

    /** 字段级错误列表 */
    private List<FieldErrorItem> fieldErrors = new ArrayList<>();

    public static ErrorResult of(String code, String message) {
        ErrorResult result = new ErrorResult();
        result.setCode(code);
        result.setMessage(message);
        result.setSeverity("error");
        return result;
    }

    public void addFieldError(String field, String code, String message, String severity) {
        fieldErrors.add(new FieldErrorItem(field, code, message, severity));
    }
}
