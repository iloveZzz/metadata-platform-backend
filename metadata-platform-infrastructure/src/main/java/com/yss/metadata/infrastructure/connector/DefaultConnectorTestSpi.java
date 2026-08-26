package com.yss.metadata.infrastructure.connector;

import com.yss.metadata.domain.connector.model.ConnectErrorType;
import com.yss.metadata.domain.connector.model.ConnectTestResult;
import com.yss.metadata.domain.connector.model.Connector;
import com.yss.metadata.domain.connector.model.ConnectorType;
import com.yss.metadata.domain.connector.model.Dialect;
import com.yss.metadata.domain.connector.spi.ConnectorTestSpi;
import org.springframework.stereotype.Component;

/**
 * 默认连接测试适配器。
 *
 * <p>seam-deferred 契约：
 * <ul>
 *   <li>GaussDB 方言连接 PoC 未认证——明确提示不支持，不伪装支持；</li>
 *   <li>连接器物理接入外部源（真实网络握手/认证）随 WU-01-03 与 PoC 落地。</li>
 * </ul></p>
 */
@Component
public class DefaultConnectorTestSpi implements ConnectorTestSpi {

    @Override
    public ConnectTestResult test(Connector connector) {
        if (connector.getType() == ConnectorType.GAUSSDB || connector.getDialect() == Dialect.GAUSSDB) {
            return ConnectTestResult.failure(ConnectErrorType.DIALECT,
                    "GaussDB 方言连接尚未通过 PoC 认证，暂不支持该方言连接测试（不伪装支持）");
        }
        return ConnectTestResult.failure(ConnectErrorType.NETWORK,
                "连接器物理接入外部源尚未实现（seam-deferred），请以 PoC 验证连接配置");
    }
}
