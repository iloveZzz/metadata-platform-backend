package com.yss.metadata.application.integration.support;

import com.yss.metadata.domain.connector.model.ConnectTestResult;
import com.yss.metadata.domain.integration.model.GravitinoEndpoint;
import com.yss.metadata.domain.integration.spi.GravitinoGateway;

/**
 * Gravitino 防腐层测试替身（应用/契约测试 seam；可配置测试结果）。
 */
public class FakeGravitinoGateway implements GravitinoGateway {

    private ConnectTestResult result;

    public void setResult(ConnectTestResult result) {
        this.result = result;
    }

    @Override
    public ConnectTestResult testConnection(GravitinoEndpoint endpoint) {
        return result;
    }
}
