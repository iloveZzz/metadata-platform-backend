package com.yss.datasecurity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaticAlgorithmVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String functionName;
    private String displayName;
    private String algorithmType;
    private String description;
    private String signature;
    private List<String> supportedEngines;
    private String sampleInput;
    private String sampleOutput;
    private String sqlExample;
}
