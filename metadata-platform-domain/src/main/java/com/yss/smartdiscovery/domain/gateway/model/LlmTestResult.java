package com.yss.smartdiscovery.domain.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmTestResult implements Serializable {
    private Boolean connected;
    private String provider;
    private Integer latencyMs;
    private Integer tokenConsumed;
    private Boolean metadataIsolationPass;
    private String message;
}
