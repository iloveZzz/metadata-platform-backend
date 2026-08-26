package com.yss.datamiddle.dqinsight.client.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 字段级错误项（冻结 OpenAPI Error.fieldErrors 元素；CSV 行号 field = "row:N"）。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FieldErrorItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** field path（如 results.3.ruleType、CSV 行号 row:12） */
    private String field;

    /** 错误码（如 err.dq.csv.schema） */
    private String code;

    /** 错误信息（脱敏） */
    private String message;

    /** 严重程度（error / warning） */
    private String severity;

    public static FieldErrorItem of(String field, String code, String message) {
        return new FieldErrorItem(field, code, message, "error");
    }
}
