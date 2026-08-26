package com.yss.metadata.domain.integration.spi;

import com.yss.metadata.domain.integration.model.DataHubEndpoint;
import com.yss.metadata.domain.integration.model.DataHubExportResult;

/**
 * DataHub 导出器端口（集成域；Domain 定义，Infrastructure 实现）。
 *
 * <p>元数据导出到 DataHub（互导验证，FR-021）；防腐层隔离外部模型。
 * 真实 DataHub 写入客户端 seam-deferred（PoC 联调后接入）；当前实现返回失败提示，
 * 不伪装导出成功。</p>
 */
public interface DataHubExporter {

    /**
     * 导出元数据到 DataHub。
     *
     * @param endpoint DataHub 目标端点
     * @param operator 触发人（审计上下文）
     * @return 导出结果（成功/失败 + 文案）
     */
    DataHubExportResult export(DataHubEndpoint endpoint, String operator);
}
