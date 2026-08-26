package com.yss.datamiddle.semantic.term.exception;

/**
 * 字段级校验业务异常（HTTP 422，冻结契约 Validation 响应）。
 *
 * <p>携带统一错误体所需的 code / severity 与 fieldErrors 明细
 * （field path / code / message / severity，与冻结契约 Error schema 对齐）。</p>
 */
public class BusinessValidationException extends RuntimeException {

    /** 顶层业务错误码（YSS 惯例 PARAM_VALIDATION_ERROR） */
    private final String code;
    /** 字段路径，如 name / aliases[0] */
    private final String field;
    /** 字段级错误码，如 TERM_NAME_DUPLICATE / REQUIRED */
    private final String fieldCode;
    /** 字段级错误信息 */
    private final String fieldMessage;

    public BusinessValidationException(String code, String field, String fieldCode,
                                       String fieldMessage) {
        super(fieldMessage);
        this.code = code;
        this.field = field;
        this.fieldCode = fieldCode;
        this.fieldMessage = fieldMessage;
    }

    public String getCode() {
        return code;
    }

    public String getField() {
        return field;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public String getFieldMessage() {
        return fieldMessage;
    }
}
