package com.yss.smartdiscovery.application.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SandboxResultDTO implements Serializable {
    private String matchedTagId;
    private String matchedTagName;
    private Double confidence;
    private String explanation;
    private Boolean l1RegexHit;
    private Boolean l2GlossaryHit;
    private Boolean l3LlmHit;
}
