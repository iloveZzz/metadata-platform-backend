package com.yss.metadata.domain.collector.spi;

import com.yss.metadata.domain.collector.model.CollectorExecutionResult;
import com.yss.metadata.domain.collector.model.CollectorTask;

/**
 * 采集执行 SPI 端口（Domain 定义，Infrastructure 适配）。
 *
 * <p>物理采集执行（连接数据源读取元数据 → 资产/版本入库）为合同 seam-deferred：
 * Gravitino 采集上游随切片 05 集成，测试环境使用 Fake seam。</p>
 */
public interface CollectorExecutionSpi {

    /**
     * 执行一次采集，返回成功或携带失败原因的结果。
     */
    CollectorExecutionResult execute(CollectorTask task);
}
