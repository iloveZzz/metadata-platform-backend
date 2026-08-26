package com.yss.datamiddle.dqinsight.rest.exception;

import com.yss.datamiddle.dqinsight.client.vo.FieldErrorItem;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 统一错误体（冻结 OpenAPI Error schema：code / message / severity / fieldErrors）。
 *
 * <p>与 metadata-platform 冻结约定一致；403 / 409 / 422 / 413 均走 yss-exception 统一错误体。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class ErrorResponseVO {

    /** 业务错误码（如 err.dq.batch.duplicate） */
    private String code;

    /** 错误信息（脱敏） */
    private String message;

    /** 严重程度（error / warning） */
    private String severity;

    /** 字段级错误 */
    private List<FieldErrorItem> fieldErrors = new ArrayList<>();

    public static ErrorResponseVO of(String code, String message) {
        ErrorResponseVO body = new ErrorResponseVO();
        body.code = code;
        body.message = message;
        body.severity = "error";
        return body;
    }

    public static ErrorResponseVO of(String code, String message, List<FieldErrorItem> fieldErrors) {
        ErrorResponseVO body = of(code, message);
        body.fieldErrors = fieldErrors == null ? Collections.emptyList() : fieldErrors;
        return body;
    }
}
