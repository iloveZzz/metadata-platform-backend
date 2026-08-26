package com.yss.metadata.infrastructure.connector;

import com.yss.cloud.dto.result.SingleResult;
import com.yss.datamiddleds.client.dto.datasource.ConnectionTestVO;
import com.yss.datamiddleds.client.feign.ConnectionTestFeignClient;
import com.yss.metadata.domain.connector.model.ConnectErrorType;
import com.yss.metadata.domain.connector.model.ConnectTestResult;
import com.yss.metadata.domain.connector.model.Connector;
import com.yss.metadata.domain.connector.model.ConnectorType;
import com.yss.metadata.domain.connector.model.Dialect;
import com.yss.metadata.domain.connector.spi.ConnectorTestSpi;
import com.yss.metadata.infrastructure.collector.convertor.MetadataCollectorConvertor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 基于 datamiddle-ds-client 的远端数据源连通性测试 SPI 适配器。
 */
@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class RemoteConnectorTestSpiImpl implements ConnectorTestSpi {

    private final ConnectionTestFeignClient connectionTestFeignClient;
    private final MetadataCollectorConvertor metadataCollectorConvertor;

    @Override
    public ConnectTestResult test(Connector connector) {
        if (connector.getType() == ConnectorType.GAUSSDB || connector.getDialect() == Dialect.GAUSSDB) {
            return ConnectTestResult.failure(ConnectErrorType.DIALECT,
                    "GaussDB 方言连接尚未通过 PoC 认证，暂不支持该方言连接测试（不伪装支持）");
        }

        String datasourceId = connector.getId();
        if (datasourceId == null || datasourceId.trim().isEmpty()) {
            return ConnectTestResult.failure(ConnectErrorType.NETWORK, "数据源 ID 不能为空");
        }

        try {
            SingleResult<ConnectionTestVO> result = connectionTestFeignClient.testDataSourceConnection(datasourceId);
            if (result != null && result.isSuccess() && result.getData() != null) {
                return metadataCollectorConvertor.toConnectTestResult(result.getData());
            }
            String message = result != null ? result.getMessage() : "远端连通性测试返回空响应";
            return ConnectTestResult.failure(ConnectErrorType.NETWORK, message);
        } catch (Exception e) {
            log.error("远端连通性测试调用异常, datasourceId={}, error={}", datasourceId, e.getMessage(), e);
            return ConnectTestResult.failure(ConnectErrorType.NETWORK, "数据源服务通信异常: " + e.getMessage());
        }
    }
}
