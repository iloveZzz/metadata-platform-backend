package com.yss.metadata.application.integration.support;

import com.yss.metadata.domain.integration.model.DataHubEndpoint;
import com.yss.metadata.domain.integration.model.DataHubExportResult;
import com.yss.metadata.domain.integration.spi.DataHubExporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DataHub 导出器测试替身（应用/契约测试 seam；可配置导出结果并记录调用）。
 */
public class FakeDataHubExporter implements DataHubExporter {

    private DataHubExportResult result = DataHubExportResult.failure("未配置测试结果");

    private final List<String> calls = new ArrayList<>();

    public void setResult(DataHubExportResult result) {
        this.result = result;
    }

    public List<String> getCalls() {
        return Collections.unmodifiableList(calls);
    }

    @Override
    public DataHubExportResult export(DataHubEndpoint endpoint, String operator) {
        calls.add(endpoint.getEndpoint() + "@" + operator);
        return result;
    }
}
