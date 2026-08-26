package com.yss.datasecurity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaticMaskTestResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String functionName;
    private String rawValue;
    private String maskedValue;
    private Long costMs;
    private String algorithmType;
    private String sqlSnippet;
}
