package com.yss.metadata.rest.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 字段级错误项（冻结 OpenAPI Error.fieldErrors items）。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FieldErrorItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 字段路径 */
    private String field;

    /** 错误码 */
    private String code;

    /** 错误信息 */
    private String message;

    /** 严重级别 */
    private String severity;
}
