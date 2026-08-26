package com.yss.metadata.repository.gateway.impl;

import com.yss.metadata.domain.integration.gateway.IntegrationConfigGateway;
import com.yss.metadata.domain.integration.model.IntegrationConfig;
import com.yss.metadata.repository.IntegrationConfigRepository;
import com.yss.metadata.infrastructure.convertor.IntegrationConfigConvertor;
import com.yss.metadata.repository.entity.IntegrationConfigPO;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 集成配置仓储实现（MyBatis-Plus；integration_config 单例行 id=1）。
 */
@Repository
public class IntegrationConfigGatewayImpl implements IntegrationConfigGateway {

    private final IntegrationConfigRepository integrationConfigRepository;
    private final IntegrationConfigConvertor integrationConfigConvertor;

    @Autowired
    public IntegrationConfigGatewayImpl(IntegrationConfigRepository integrationConfigRepository) {
        this(integrationConfigRepository, Mappers.getMapper(IntegrationConfigConvertor.class));
    }

    public IntegrationConfigGatewayImpl(IntegrationConfigRepository integrationConfigRepository, IntegrationConfigConvertor integrationConfigConvertor) {
        this.integrationConfigRepository = integrationConfigRepository;
        this.integrationConfigConvertor = integrationConfigConvertor != null ? integrationConfigConvertor : Mappers.getMapper(IntegrationConfigConvertor.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IntegrationConfig> find() {
        IntegrationConfigPO po = integrationConfigRepository.selectById(IntegrationConfig.SINGLETON_ID);
        return po == null ? Optional.empty() : Optional.of(integrationConfigConvertor.toDomain(po));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IntegrationConfig save(IntegrationConfig config) {
        IntegrationConfigPO po = integrationConfigConvertor.toPO(config);
        if (integrationConfigRepository.selectById(po.getId()) != null) {
            integrationConfigRepository.updateById(po);
        } else {
            integrationConfigRepository.insert(po);
        }
        return integrationConfigConvertor.toDomain(po);
    }
}
