package com.yss.metadata.repository.gateway.impl;

import com.yss.metadata.domain.connector.model.ConnectErrorType;
import com.yss.metadata.domain.connector.model.ConnectTestResult;
import com.yss.metadata.domain.integration.model.GravitinoEndpoint;
import com.yss.metadata.domain.integration.spi.GravitinoGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Gravitino 防腐层默认实现（seam-deferred，PoC 未认证）。
 *
 * <p>真实 Gravitino REST/OpenLineage 消费客户端随 PoC 接入（同切片 03 GaussDB
 * 方言先例：不伪装接入）；当前测试连接返回分类失败提示（network），
 * 端点未配置亦返回分类失败（422 语义由 Web 层映射）。</p>
 */
@Component
@Slf4j
public class DefaultGravitinoGateway implements GravitinoGateway {

    @Override
    public ConnectTestResult testConnection(GravitinoEndpoint endpoint) {
        if (endpoint == null || endpoint.getEndpoint() == null || endpoint.getEndpoint().trim().isEmpty()) {
            return ConnectTestResult.failure(ConnectErrorType.NETWORK, "未配置 Gravitino 端点地址");
        }
        log.warn("Gravitino 连接测试 seam-deferred（PoC 未接入），endpoint={}", endpoint.getEndpoint());
        return ConnectTestResult.failure(ConnectErrorType.NETWORK,
                "Gravitino 客户端未接入（PoC seam-deferred，切片 05 仅防腐层契约）");
    }
}
