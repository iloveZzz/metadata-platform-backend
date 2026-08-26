package com.yss.metadata.domain.connector.spi;

import com.yss.metadata.domain.connector.model.ConnectTestResult;
import com.yss.metadata.domain.connector.model.Connector;

/**
 * 连接测试 SPI 端口（Domain 定义，Infrastructure 适配物理连接）。
 *
 * <p>物理接入外部源为合同 seam-deferred：测试环境使用 InMemory/Test 源，
 * 生产适配器在 GaussDB 方言 PoC 未认证时明确提示、不伪装支持。</p>
 */
public interface ConnectorTestSpi {

    /**
     * 测试连接器连通性，返回分类结果（成功或 network/credential/dialect 失败）。
     */
    ConnectTestResult test(Connector connector);
}
