package com.yss.metadata.domain.integration.gateway;

import com.yss.metadata.domain.integration.model.IntegrationConfig;

import java.util.Optional;

/**
 * 集成配置仓储端口（集成域；Domain 定义，Infrastructure 实现）。
 *
 * <p>integration_config 表：单例配置行（id=1）读取与保存（upsert）。</p>
 */
public interface IntegrationConfigGateway {

    /**
     * 读取单例集成配置（无配置时返回空，空结构非错误）。
     */
    Optional<IntegrationConfig> find();

    /**
     * 保存单例集成配置（upsert：id=1）。
     */
    IntegrationConfig save(IntegrationConfig config);
}
