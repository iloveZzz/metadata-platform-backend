package com.yss.datamiddle.semantic.rest.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 字段级错误项（冻结契约 Error.fieldErrors 元素）。
 */
@Getter
@Setter
@AllArgsConstructor
public class FieldErrorItem {

    /** field path，如 name / aliases[0] */
    private String field;

    /** 字段级错误码，如 TERM_NAME_DUPLICATE / REQUIRED / INVALID_ENUM */
    private String code;

    private String message;

    private String severity;
}
