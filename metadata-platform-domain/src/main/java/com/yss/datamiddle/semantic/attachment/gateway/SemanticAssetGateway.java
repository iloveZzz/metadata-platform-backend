package com.yss.datamiddle.semantic.attachment.gateway;

import java.util.Optional;

/**
 * 资产元数据防腐层网关（SL-009）。
 */
public interface SemanticAssetGateway {
    boolean existsAsset(Long assetId);
    Optional<String> getAssetName(Long assetId);
}
