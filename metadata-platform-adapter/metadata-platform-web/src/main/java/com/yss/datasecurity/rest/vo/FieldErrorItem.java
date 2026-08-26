package com.yss.datasecurity.rest.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FieldErrorItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String field;
    private String code;
    private String message;
    private String severity;
}
