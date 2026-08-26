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
public class LlmConfig implements Serializable {
    private String provider;
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private Integer autoApplyThreshold;
    private Integer timeoutMs;
}
