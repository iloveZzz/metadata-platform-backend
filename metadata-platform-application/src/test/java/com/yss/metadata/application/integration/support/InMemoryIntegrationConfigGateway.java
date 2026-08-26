package com.yss.metadata.application.integration.support;

import com.yss.metadata.domain.integration.gateway.IntegrationConfigGateway;
import com.yss.metadata.domain.integration.model.IntegrationConfig;

import java.util.Optional;

/**
 * 集成配置仓储内存实现（应用/契约测试 seam；镜像 upsert 单例行语义）。
 */
public class InMemoryIntegrationConfigGateway implements IntegrationConfigGateway {

    private IntegrationConfig config;

    public void seed(IntegrationConfig config) {
        this.config = config;
    }

    @Override
    public Optional<IntegrationConfig> find() {
        return Optional.ofNullable(config);
    }

    @Override
    public IntegrationConfig save(IntegrationConfig config) {
        this.config = config;
        return config;
    }
}
