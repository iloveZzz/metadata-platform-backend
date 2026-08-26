package com.yss.datasecurity.rest.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ErrorResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private String message;
    private String severity;
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
