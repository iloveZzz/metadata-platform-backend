package com.yss.metadata.rest.support;

import com.yss.metadata.domain.integration.model.DataHubEndpoint;
import com.yss.metadata.domain.integration.model.DataHubExportResult;
import com.yss.metadata.domain.integration.spi.DataHubExporter;

/**
 * DataHub 导出器测试替身（Web 契约测试 seam；可配置导出结果）。
 */
public class FakeDataHubExporter implements DataHubExporter {

    private DataHubExportResult result = DataHubExportResult.failure("未配置测试结果");

    public void setResult(DataHubExportResult result) {
        this.result = result;
    }

    @Override
    public DataHubExportResult export(DataHubEndpoint endpoint, String operator) {
        return result;
    }
}
