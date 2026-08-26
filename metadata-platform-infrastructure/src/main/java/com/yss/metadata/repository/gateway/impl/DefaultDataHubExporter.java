package com.yss.metadata.repository.gateway.impl;

import com.yss.metadata.domain.integration.model.DataHubEndpoint;
import com.yss.metadata.domain.integration.model.DataHubExportResult;
import com.yss.metadata.domain.integration.spi.DataHubExporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * DataHub 导出器默认实现（seam-deferred，PoC 联调未认证）。
 *
 * <p>真实 DataHub 写入客户端随 PoC 接入；当前返回失败提示，不伪装导出成功
 * （导出任务标记 failed，202 任务状态承载，与切片 04 传播失败语义同构）。</p>
 */
@Component
@Slf4j
public class DefaultDataHubExporter implements DataHubExporter {

    @Override
    public DataHubExportResult export(DataHubEndpoint endpoint, String operator) {
        log.warn("DataHub 导出 seam-deferred（PoC 未接入），endpoint={}, operator={}",
                endpoint == null ? null : endpoint.getEndpoint(), operator);
        return DataHubExportResult.failure("DataHub 客户端未接入（PoC seam-deferred，切片 05 仅导出器契约）");
    }
}
