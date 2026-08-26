package com.yss.smartdiscovery.rest;

import com.yss.cloud.dto.result.SingleResult;
import com.yss.smartdiscovery.application.service.LlmGatewayAppService;
import com.yss.smartdiscovery.domain.gateway.model.LlmConfig;
import com.yss.smartdiscovery.domain.gateway.model.LlmTestResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/smart-discovery/config/llm")
@RequiredArgsConstructor
public class LlmGatewayConfigController {

    private final LlmGatewayAppService llmGatewayAppService;

    @GetMapping
    public SingleResult<LlmConfig> getLlmConfig() {
        LlmConfig config = llmGatewayAppService.getConfig();
        if (config != null) {
            // 掩码脱敏
            config.setApiKey("sk-************************");
        }
        return SingleResult.of(config);
    }

    @PutMapping
    public SingleResult<LlmConfig> updateLlmConfig(@Valid @RequestBody LlmConfig config) {
        return SingleResult.of(llmGatewayAppService.updateConfig(config));
    }

    @PostMapping("/test")
    public SingleResult<LlmTestResult> testLlmConnection() {
        return SingleResult.of(llmGatewayAppService.testConnection());
    }
}
