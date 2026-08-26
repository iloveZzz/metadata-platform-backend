package com.yss.metadata.application.connector.support;

import com.yss.metadata.domain.connector.model.ConnectTestResult;
import com.yss.metadata.domain.connector.model.Connector;
import com.yss.metadata.domain.connector.spi.ConnectorTestSpi;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 连接测试 SPI 测试替身（seam-deferred）。
 *
 * <p>按测试场景预先编排返回结果（成功 / 网络 / 凭据 / 方言），
 * 替代真实外部数据源物理连接。</p>
 */
public class FakeConnectorTestSpi implements ConnectorTestSpi {

    private final Deque<ConnectTestResult> results = new ArrayDeque<>();

    public void enqueue(ConnectTestResult result) {
        results.addLast(result);
    }

    @Override
    public ConnectTestResult test(Connector connector) {
        if (results.isEmpty()) {
            return ConnectTestResult.success("连接成功");
        }
        return results.removeFirst();
    }
}
