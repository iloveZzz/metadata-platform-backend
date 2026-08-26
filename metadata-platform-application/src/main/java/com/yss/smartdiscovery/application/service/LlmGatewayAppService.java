package com.yss.smartdiscovery.application.service;

import com.yss.smartdiscovery.domain.gateway.LlmGateway;
import com.yss.smartdiscovery.domain.gateway.model.LlmConfig;
import com.yss.smartdiscovery.domain.gateway.model.LlmTestResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LlmGatewayAppService {

    private final LlmGateway llmGateway;

    public LlmConfig getConfig() {
        return llmGateway.getConfig();
    }

    public LlmConfig updateConfig(LlmConfig config) {
        return llmGateway.updateConfig(config);
    }

    public LlmTestResult testConnection() {
        return llmGateway.testConnection();
    }
}
