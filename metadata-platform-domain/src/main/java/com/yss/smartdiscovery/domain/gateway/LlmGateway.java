package com.yss.smartdiscovery.domain.gateway;

import com.yss.smartdiscovery.domain.gateway.model.LlmConfig;
import com.yss.smartdiscovery.domain.gateway.model.LlmTestResult;

public interface LlmGateway {
    LlmConfig getConfig();
    LlmConfig updateConfig(LlmConfig next);
    LlmTestResult testConnection();
}
