package com.yss.datamiddle.semantic.rest.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一错误体（冻结契约 Error schema；code / message / severity / fieldErrors）。
 *
 * <p>data 字段为 409 VERSION_CONFLICT 携带最新对象的扩展承载（CT-04），
 * 冻结契约 Error schema 未定义该字段，属实现期对齐项（待主控评审确认）。</p>
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String code;

    private String message;

    private String severity;

    private List<FieldErrorItem> fieldErrors;

    /** 409 VERSION_CONFLICT 时携带最新对象（TermVO） */
    private Object data;

    public static ErrorResponse of(String code, String message, String severity) {
        ErrorResponse response = new ErrorResponse();
        response.code = code;
        response.message = message;
        response.severity = severity;
        return response;
    }

    public ErrorResponse addFieldError(FieldErrorItem item) {
        if (fieldErrors == null) {
            fieldErrors = new ArrayList<>();
        }
        fieldErrors.add(item);
        return this;
    }
}
