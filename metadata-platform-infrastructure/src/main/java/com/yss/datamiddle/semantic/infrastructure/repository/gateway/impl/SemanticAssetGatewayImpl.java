package com.yss.datamiddle.semantic.infrastructure.repository.gateway.impl;

import com.yss.datamiddle.semantic.attachment.gateway.SemanticAssetGateway;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 资产防腐层客户端网关实现（mock 消费冻结契约 GET /api/assets）。
 */
@Component
public class SemanticAssetGatewayImpl implements SemanticAssetGateway {

    @Override
    public boolean existsAsset(Long assetId) {
        return assetId != null && assetId > 0;
    }

    @Override
    public Optional<String> getAssetName(Long assetId) {
        if (assetId == null || assetId <= 0) {
            return Optional.empty();
        }
        return Optional.of("asset_table_" + assetId);
    }
}
